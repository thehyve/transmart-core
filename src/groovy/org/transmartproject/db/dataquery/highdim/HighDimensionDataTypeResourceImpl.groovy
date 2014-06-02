package org.transmartproject.db.dataquery.highdim

import grails.orm.HibernateCriteriaBuilder
import groovy.util.logging.Log4j

import org.hibernate.ScrollMode
import org.hibernate.engine.SessionImplementor
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.EmptySetException
import org.transmartproject.core.exceptions.UnsupportedByDataTypeException
import org.transmartproject.db.dataquery.highdim.assayconstraints.PlatformConstraint
import org.transmartproject.db.dataquery.highdim.dataconstraints.CriteriaDataConstraint
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection

@Log4j
class HighDimensionDataTypeResourceImpl implements HighDimensionDataTypeResource {

    protected HighDimensionDataTypeModule module

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
        assayConstraints << new PlatformConstraint(
                platformNames: module.platformMarkerTypes)
                                                                  
        def assayQuery = new AssayQuery(assayConstraints)
        List<AssayColumn> assays

        assays = assayQuery.retrieveAssays()
        if (assays.empty) {
            throw new EmptySetException(
                    'No assays satisfy the provided criteria')
        }

        HibernateCriteriaBuilder criteriaBuilder =
            module.prepareDataQuery(projection, openSession())

        criteriaBuilder.with {
            'in' 'assay.id', assays*.id
        }

        /* apply changes to criteria from projection, if any */
        if (projection instanceof CriteriaProjection) {
            projection.doWithCriteriaBuilder criteriaBuilder
        }

        /* apply data constraints */
        for (CriteriaDataConstraint dataConstraint in dataConstraints) {
            dataConstraint.doWithCriteriaBuilder criteriaBuilder
        }

        module.transformResults(
                criteriaBuilder.instance.scroll(ScrollMode.FORWARD_ONLY),
                assays,
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
}
