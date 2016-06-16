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
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.EmptySetException
import org.transmartproject.core.exceptions.UnsupportedByDataTypeException
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.dataquery.highdim.assayconstraints.MarkerTypeCriteriaConstraint
import org.transmartproject.db.dataquery.highdim.dataconstraints.CriteriaDataConstraint
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection
import org.transmartproject.db.ontology.I2b2

import static org.transmartproject.db.util.GormWorkarounds.getHibernateInCriterion

@Log4j
class HighDimensionDataTypeResourceImpl implements HighDimensionDataTypeResource {

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
        ''', [markerTypes : module.platformMarkerTypes, resultInstanceId: queryResult.id]
    }


    @Override
    public String toString() {
        return "HighDimensionDataTypeResourceImpl{dataTypeName=$dataTypeName, " +
                "identity=${System.identityHashCode(this)}"
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        HighDimensionDataTypeResourceImpl that = (HighDimensionDataTypeResourceImpl) o

        if (dataTypeName != that.dataTypeName) return false

        return true
    }

    int hashCode() {
        return dataTypeName.hashCode()
    }
}
