/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.multidimquery

import groovy.transform.Immutable
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.User

interface MultiDimensionalDataResource {

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
    Hypercube retrieveData(Map args, String dataType, Collection<MDStudy> accessibleStudies)

    Dimension getDimension(String name)


    Long count(MultiDimConstraint constraint, User user)
    Long cachedCount(MultiDimConstraint constraint, User user)

    List<Patient> listPatients(MultiDimConstraint constraint, User user)

    QueryResult createPatientSet(String name, MultiDimConstraint constraint, User user, String constraintText, String apiVersion) 

    QueryResult findPatientSet(Long patientSetId, User user)

    Long patientCount(MultiDimConstraint constraint, User user)
    Long cachedPatientCount(MultiDimConstraint constraint, User user)

    Number aggregate(AggregateType type, MultiDimConstraint constraint, User user)

    Hypercube highDimension(
            MultiDimConstraint assayConstraint_,
            MultiDimConstraint biomarkerConstraint,
            String projectionName,
            User user,
            String type)

    Hypercube retrieveClinicalData(MultiDimConstraint constraint, User user)

    RequestConstraintsAndVersion getPatientSetRequestConstraintsAndApiVersion(long id)

    @Immutable class RequestConstraintsAndVersion { String constraints; String version }
}