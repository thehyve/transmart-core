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
import groovy.util.logging.Log4j
import org.hibernate.ScrollMode
import org.hibernate.engine.SessionImplementor
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.EmptySetException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.UnsupportedByDataTypeException
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.querytool.ConstraintByOmicsValue
import org.transmartproject.core.querytool.ConstraintByOmicsValue.Operator
import org.transmartproject.core.querytool.HighDimensionFilterType
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.dataquery.highdim.assayconstraints.MarkerTypeCriteriaConstraint
import org.transmartproject.db.dataquery.highdim.dataconstraints.CriteriaDataConstraint
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection
import org.transmartproject.db.ontology.I2b2

import static org.transmartproject.db.util.GormWorkarounds.getHibernateInCriterion

@Log4j
class HighDimensionDataTypeResourceImpl implements HighDimensionDataTypeResource {

    HighDimensionResource highDimensionResource
    protected HighDimensionDataTypeModule module

    private static final int FETCH_SIZE = 10000

    HighDimensionDataTypeResourceImpl(HighDimensionDataTypeModule module) {
        this.module = module
    }

    @Override
    String getDataTypeName() {
        module.name
    }

    @Override
    String getDataTypeDescription() {
        module.description
    }

    protected SessionImplementor openSession() {
        module.sessionFactory.openStatelessSession()
    }

    protected getAssayProperty() {
        /* we could change this to inspect the associations of the root type and
         * find the name of the association linking to DeSubjectSampleMapping */
        'assay'
    }

    @Override
    TabularResult retrieveData(List<AssayConstraint> assayConstraints,
                                 List<DataConstraint> dataConstraints,
                                 Projection projection) {

        // Each module should only return assays that match 
        // the markertypes specified, in addition to the 
        // constraints given
        assayConstraints << new MarkerTypeCriteriaConstraint(
                platformNames: module.platformMarkerTypes)

        def assaysQuery = new AssayQuery(assayConstraints)

        def assays = assaysQuery.list()
        if (!assays) {
            throw new EmptySetException(
                    'No assays satisfy the provided criteria')
        }

        HibernateCriteriaBuilder criteriaBuilder =
            module.prepareDataQuery(projection, openSession())

        //We have to specify projection explicitly because of the grails bug
        //https://jira.grails.org/browse/GRAILS-12107
        criteriaBuilder.add(getHibernateInCriterion('assay.id', assaysQuery.forIds()))

        /* apply changes to criteria from projection, if any */
        if (projection instanceof CriteriaProjection) {
            projection.doWithCriteriaBuilder criteriaBuilder
        }

        /* apply data constraints */
        for (CriteriaDataConstraint dataConstraint in dataConstraints) {
            dataConstraint.doWithCriteriaBuilder criteriaBuilder
        }

        criteriaBuilder.instance.fetchSize = FETCH_SIZE

        module.transformResults(
                criteriaBuilder.instance.scroll(ScrollMode.FORWARD_ONLY),
                assays.collect { new AssayColumnImpl(it) },
                projection)
    }

    @Override
    Set<String> getSupportedAssayConstraints() {
        module.supportedAssayConstraints
    }

    @Override
    Set<String> getSupportedDataConstraints() {
        module.supportedDataConstraints
    }

    @Override
    Set<String> getSupportedProjections() {
        module.supportedProjections
    }

    @Override
    AssayConstraint createAssayConstraint(Map<String, Object> params, String name)
            throws UnsupportedByDataTypeException {
        module.createAssayConstraint params, name
    }

    @Override
    DataConstraint createDataConstraint(Map<String, Object> params, String name)
            throws UnsupportedByDataTypeException {
        module.createDataConstraint params, name
    }

    @Override
    Projection createProjection(Map<String, Object> params, String name)
            throws UnsupportedByDataTypeException {
        module.createProjection params, name
    }

    @Override
    Projection createProjection(String name)
            throws UnsupportedByDataTypeException{
        createProjection([:], name)
    }

    @Override
    boolean matchesPlatform(Platform platform) {
        platform.markerType in module.platformMarkerTypes
    }

    @Override
    Set<OntologyTerm> getAllOntologyTermsForDataTypeBy(QueryResult queryResult) {
        I2b2.executeQuery '''
            from I2b2 where code in
                (select
                    distinct ssm.conceptCode
                from QtPatientSetCollection ps, DeSubjectSampleMapping ssm
                inner join ssm.platform as p
                where p.markerType in (:markerTypes)
                    and ssm.patient = ps.patient
                    and ps.resultInstance.id = :resultInstanceId)
        ''', [markerTypes: module.platformMarkerTypes, resultInstanceId: queryResult.id]
    }

    @Override
    List<String> searchAnnotation(String concept_code, String search_term, String search_property) {
        module.searchAnnotation(concept_code, search_term, search_property)
    }

    @Override
    List<String> getSearchableAnnotationProperties() {
        module.getSearchableAnnotationProperties()
    }

    @Override
    HighDimensionFilterType getHighDimensionFilterType() {
        module.getHighDimensionFilterType()
    }

    @Override
    List<String> getSearchableProjections() {
        module.getSearchableProjections()
    }

    @Override
    def getDistribution(ConstraintByOmicsValue constraint, String concept_key, Long result_instance_id = null) {
        def dataConstraints = []
        def assayConstraints = []
        dataConstraints.add(createDataConstraint([property: constraint.property, term: constraint.selector, concept_key: concept_key], DataConstraint.ANNOTATION_CONSTRAINT))
        assayConstraints.add(createAssayConstraint([concept_key: concept_key], AssayConstraint.ONTOLOGY_TERM_CONSTRAINT))
        if (result_instance_id != null)
            assayConstraints.add(createAssayConstraint([result_instance_id: result_instance_id], AssayConstraint.PATIENT_SET_CONSTRAINT))

        def projection
        switch (constraint.projectionType) {
            case ConstraintByOmicsValue.ProjectionType.LOGINTENSITY:
                projection = createProjection([:], Projection.LOG_INTENSITY_PROJECTION)
                break
            case ConstraintByOmicsValue.ProjectionType.RAWINTENSITY:
                projection = createProjection([:], Projection.DEFAULT_REAL_PROJECTION)
                break
            case ConstraintByOmicsValue.ProjectionType.ZSCORE:
                projection = createProjection([:], Projection.ZSCORE_PROJECTION)
                break
            case ConstraintByOmicsValue.ProjectionType.LOG_NORMALIZED_READCOUNT:
                projection = createProjection([:], Projection.LOG_INTENSITY_PROJECTION)
                break
            default:
                log.error("Unsupported projection for getDistrubtion: " + constraint.projectionType.value)
                throw new InvalidArgumentsException("Unsupported projection for getDistrubtion: " + constraint.projectionType.value)
        }
        def retrieved = retrieveData(assayConstraints, dataConstraints, projection)
        def data = [:]
        // transform to a map where the keys are patient ids, and the values are the values of probes of the patient
        retrieved.rows.each { row ->
            row.assayIndexMap.each { assay, index ->
                if (row.data[index] != null) data.get(assay.patient.id, []).add(row.data[index])
            }
        }
        retrieved.close()
        // calculate the mean probe value for each patient
        def aggregator = highDimensionConstraintValuesAggregator(constraint)
        data.each {it.setValue(aggregator(it.getValue()))} // set the value of each Map.Entry to the aggregated value
        def filter = highDimensionConstraintClosure(constraint)
        data.findAll {filter(it.getValue())}
    }
    
    protected def highDimensionConstraintValuesAggregator(ConstraintByOmicsValue constraint) {
        {values -> values.sum() / values.size()}
    }

    protected def highDimensionConstraintClosure(ConstraintByOmicsValue constraint) {
        if (getHighDimensionFilterType() == HighDimensionFilterType.SINGLE_NUMERIC) {
            // default aggregator for numeric is average
            // this should be parameterized in the future
            if (constraint.operator != null && constraint.constraint != null) {
                switch (constraint.operator) {
                    case ConstraintByOmicsValue.Operator.BETWEEN:
                        def limits = constraint.constraint.split(':')*.toDouble()
                        return {value -> limits[0] <= value && value <= limits[1]}
                        break;
                    case ConstraintByOmicsValue.Operator.EQUAL_TO:
                        def limit = constraint.constraint.toDouble()
                        return {value -> limit == value}
                        break;
                    case ConstraintByOmicsValue.Operator.GREATER_OR_EQUAL_TO:
                        def limit = constraint.constraint.toDouble()
                        return {value -> limit <= value}
                        break;
                    case ConstraintByOmicsValue.Operator.GREATER_THAN:
                        def limit = constraint.constraint.toDouble()
                        return {value -> limit < value}
                        break;
                    case ConstraintByOmicsValue.Operator.LOWER_OR_EQUAL_TO:
                        def limit = constraint.constraint.toDouble()
                        return {value -> limit >= value}
                        break;
                    case ConstraintByOmicsValue.Operator.LOWER_THAN:
                        def limit = constraint.constraint.toDouble()
                        return {value -> limit > value}
                        break;
                }
            }
            else
                return {row -> true}
        }
        else
            return {row -> true}
    }
}
