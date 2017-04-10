/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.multidimquery

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.User

abstract trait MultiDimensionalDataResource {

    /**
     * @param accessibleStudies: The studies the current user has access to.
     * @param dataType: The string identifying the data type. "clinical" for clinical data, for high dimensional data
     * the appropriate identifier string.
     * @param constraints: (nullable) A list of Constraint-s. If null, selects all the data in the database.
     * @param dimensions: (nullable) A list of Dimension-s to select. Only dimensions valid for the selected studies
     * will actually be applied. If null, select all available dimensions.
     *
     * Not yet implemented:
     * @param sort
     * @param pack
     * @param preloadDimensions
     *
     * @return a Hypercube result
     */
    abstract Hypercube retrieveData(Map args, String dataType, Collection<MDStudy> accessibleStudies)

    abstract Dimension getDimension(String name)


    abstract Long count(MultiDimConstraint constraint, User user)
    abstract Long cachedCount(MultiDimConstraint constraint, User user)

    abstract List<Patient> listPatients(MultiDimConstraint constraint, User user)

    abstract QueryResult createPatientSet(String name, MultiDimConstraint constraint, User user)

    abstract QueryResult findPatientSet(Long patientSetId, User user)

    abstract Long patientCount(MultiDimConstraint constraint, User user)
    abstract Long cachedPatientCount(MultiDimConstraint constraint, User user)

    abstract Number aggregate(AggregateType type, MultiDimConstraint constraint, User user)

    abstract Hypercube highDimension(
            MultiDimConstraint assayConstraint_,
            MultiDimConstraint biomarkerConstraint = null,
            String projectionName = Projection.ALL_DATA_PROJECTION,
            User user,
            String type)

    abstract Hypercube retrieveClinicalData(MultiDimConstraint constraint, User user)
}