/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.dataquery.highdim

import grails.orm.HibernateCriteriaBuilder
import org.hibernate.criterion.Order
import org.hibernate.criterion.Projections
import org.hibernate.Criteria
import org.hibernate.SessionFactory
import org.hibernate.criterion.Restrictions
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.UnsupportedByDataTypeException
import org.transmartproject.core.querytool.ConstraintByOmicsValue
import org.transmartproject.core.querytool.HighDimensionFilterType
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.querytool.QtPatientSetCollection

import javax.annotation.PostConstruct

abstract class AbstractHighDimensionDataTypeModule implements HighDimensionDataTypeModule {

    @Autowired
    SessionFactory sessionFactory

    protected List<DataRetrievalParameterFactory> assayConstraintFactories

    protected List<DataRetrievalParameterFactory> dataConstraintFactories

    protected List<DataRetrievalParameterFactory> projectionFactories

    @Autowired
    HighDimensionResourceService highDimensionResourceService

    static Map<String, Class> typesMap(Class domainClass, List<String> fields,
                                       Map<String, String> translationMap = [:]) {
        fields.collectEntries({
            [(it): domainClass.metaClass.getMetaProperty(translationMap.get(it, it)).type]
        }).asImmutable()
    }

    @PostConstruct
    void init() {
        this.highDimensionResourceService.registerHighDimensionDataTypeModule(
                name, this.&createHighDimensionResource)
    }

    HighDimensionDataTypeResource createHighDimensionResource(Map params) {
        /* params are unused; at least for now */
        new HighDimensionDataTypeResourceImpl(this)
    }

    @Lazy volatile Set<String> supportedAssayConstraints = {
        initializeFactories()
        assayConstraintFactories.inject(new HashSet()) {
                Set accum, DataRetrievalParameterFactory elem ->
                    accum.addAll elem.supportedNames
                    accum
        }
    }()

    @Lazy volatile Set<String> supportedDataConstraints = {
        initializeFactories()
        dataConstraintFactories.inject(new HashSet()) {
                Set accum, DataRetrievalParameterFactory elem ->
                    accum.addAll elem.supportedNames
                    accum
        }
    }()

    @Lazy volatile Set<String> supportedProjections = {
        initializeFactories()
        projectionFactories.inject(new HashSet()) {
                Set accum, DataRetrievalParameterFactory elem ->
                    accum.addAll elem.supportedNames
                    accum
        }
    }()

    final synchronized protected initializeFactories() {
        if (assayConstraintFactories != null) {
            return // already initialized
        }

        assayConstraintFactories = createAssayConstraintFactories()
        dataConstraintFactories  = createDataConstraintFactories()
        projectionFactories      = createProjectionFactories()
    }

    abstract protected List<DataRetrievalParameterFactory> createAssayConstraintFactories()

    abstract protected List<DataRetrievalParameterFactory> createDataConstraintFactories()

    abstract protected List<DataRetrievalParameterFactory> createProjectionFactories()

    @Override
    AssayConstraint createAssayConstraint(Map<String, Object> params, String name) {
        initializeFactories()
        for (factory in assayConstraintFactories) {
            if (factory.supports(name)) {
                return factory.createFromParameters(
                        name, params, this.&createAssayConstraint)
            }
        }

        throw new UnsupportedByDataTypeException("The data type ${this.name} " +
                "does not support the assay constraint $name")
    }

    @Override
    DataConstraint createDataConstraint(Map<String, Object> params, String name) {
        initializeFactories()
        for (factory in dataConstraintFactories) {
            if (factory.supports(name)) {
                return factory.createFromParameters(
                        name, params, this.&createDataConstraint)
            }
        }

        throw new UnsupportedByDataTypeException("The data type ${this.name} " +
                "does not support the data constraint $name")
    }

    @Override
    Projection createProjection(Map<String, Object> params, String name) {
        initializeFactories()
        for (factory in projectionFactories) {
            if (factory.supports(name)) {
                return factory.createFromParameters(
                        name, params, this.&createProjection)
            }
        }

        throw new UnsupportedByDataTypeException("The data type ${this.name} " +
                "does not support the projection $name")
    }

    final protected Map createAssayIndexMap(List assays) {
        int i = 0
        assays.collectEntries { [ it, i++ ] }
    }

    @Override
    def getDistribution(ConstraintByOmicsValue constraint, String concept_code, Long result_instance_id) {
        def search_property = constraint.property
        def projection = constraint.projectionType.value
        if (!getSearchableAnnotationProperties().contains(search_property) || !getSearchableProjections().contains(projection))
            return []
        def filter = highDimensionConstraintClosure(constraint)
        Criteria c = prepareAnnotationCriteria(constraint, concept_code)
        c.add(Restrictions.in('assay', DeSubjectSampleMapping.createCriteria().listDistinct { eq('conceptCode', concept_code) }))
        if (result_instance_id != null) {
            c.add(Restrictions.in('patient.id', QtPatientSetCollection.createCriteria().listDistinct {
                eq('resultInstance.id', result_instance_id)
                projections { property 'patient.id' }
            }))
        }
        c.setProjection(Projections.projectionList()
            .add(Projections.groupProperty('patient.id'))
            .add(Projections.alias(Projections.avg(projection), 'score'))
        )
        c.addOrder(Order.asc('score'))
        c.list().findAll {filter(it)}
    }

    protected abstract Criteria prepareAnnotationCriteria(ConstraintByOmicsValue constraint, String concept_code)

    protected def highDimensionConstraintClosure(ConstraintByOmicsValue constraint) {
        if (getHighDimensionFilterType() == HighDimensionFilterType.SINGLE_NUMERIC) {
            if (constraint.operator != null && constraint.constraint != null) {
                switch (constraint.operator) {
                    case ConstraintByOmicsValue.Operator.BETWEEN:
                        def limits = constraint.constraint.split(':')*.toDouble()
                        return {row -> limits[0] <= row[1] && row[1] <= limits[1]}
                        break;
                    case ConstraintByOmicsValue.Operator.EQUAL_TO:
                        def limit = constraint.constraint.toDouble()
                        return {row -> limit == row[1]}
                        break;
                    case ConstraintByOmicsValue.Operator.GREATER_OR_EQUAL_TO:
                        def limit = constraint.constraint.toDouble()
                        return {row -> limit <= row[1]}
                        break;
                    case ConstraintByOmicsValue.Operator.GREATER_THAN:
                        def limit = constraint.constraint.toDouble()
                        return {row -> limit < row[1]}
                        break;
                    case ConstraintByOmicsValue.Operator.LOWER_OR_EQUAL_TO:
                        def limit = constraint.constraint.toDouble()
                        return {row -> limit >= row[1]}
                        break;
                    case ConstraintByOmicsValue.Operator.LOWER_THAN:
                        def limit = constraint.constraint.toDouble()
                        return {row -> limit > row[1]}
                        break;
                }
            }
            else
                return {row -> true}
        }
        else
            return {row -> false}
    }
}
