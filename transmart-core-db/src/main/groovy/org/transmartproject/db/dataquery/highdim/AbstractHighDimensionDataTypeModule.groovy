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
import org.hibernate.SessionFactory
import org.hibernate.engine.spi.SessionImplementor
import org.hibernate.internal.CriteriaImpl
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.UnsupportedByDataTypeException
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory

import javax.annotation.PostConstruct

abstract class AbstractHighDimensionDataTypeModule implements HighDimensionDataTypeModule {

    @Autowired
    SessionFactory sessionFactory

    protected int fetchSize = 10000

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

    HibernateCriteriaBuilder prepareDataQuery(
            List<AssayColumn> assays,
            Projection projection,
            SessionImplementor session) {
        return prepareDataQuery(projection, session)
    }

    abstract HibernateCriteriaBuilder prepareDataQuery(
            Projection projection,
            SessionImplementor session)

    final protected HibernateCriteriaBuilder createCriteriaBuilder(
            Class targetClass, String alias, SessionImplementor session) {

        HibernateCriteriaBuilder builder = new HibernateCriteriaBuilder(targetClass, sessionFactory)

        /* we have to write a private here */
        if (session) {
            //force usage of a specific session (probably stateless)
            builder.criteria = new CriteriaImpl(targetClass.canonicalName,
                                                alias,
                                                session)
            builder.criteriaMetaClass = GroovySystem.metaClassRegistry.
                    getMetaClass(builder.criteria.getClass())
        } else {
            builder.createCriteriaInstance()
        }

        /* builder.instance.is(builder.criteria) */
        builder.instance.readOnly = true
        builder.instance.cacheable = false
        builder.instance.fetchSize = fetchSize

        builder
    }

    final protected Map createAssayIndexMap(List assays) {
        int i = 0
        assays.collectEntries { [ it, i++ ] }
    }
}
