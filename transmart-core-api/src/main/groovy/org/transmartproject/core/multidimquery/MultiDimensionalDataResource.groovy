/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.multidimquery

import org.transmartproject.core.multidimquery.query.BiomarkerConstraint
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.User

interface MultiDimensionalDataResource {

    /**
     * @param user : The current user.
     * @param dataType : The string identifying the data type. "clinical" for clinical data, for high dimensional data
     * the appropriate identifier string.
     * @param constraints : (nullable) A list of Constraint-s. If null, selects all the data in the database.
     * @param dimensions : (nullable) A list of Dimension-s to select. Only dimensions valid for the selected studies
     * will actually be applied. If null, select all available dimensions.
     *
     * Not yet implemented:
     * @param sort
     *
     * @return a Hypercube result
     */
    Hypercube retrieveData(Map args, String dataType, User user)

    Dimension getDimension(String name)

    Iterable getDimensionElements(Dimension dimension, Constraint constraint, User user)

    /**
     * @description Function for creating a patient set consisting of patients for which there are observations
     * that are specified by <code>query</code>.
     */
    QueryResult createPatientSetQueryResult(String name, Constraint constraint, User user, String apiVersion)

    /**
     * The same as {@link this.createPatientSetQueryResult}, but first ties to reuse existing patient set that satisfies
     * provided constraints
     * @return A new ore reused patient set.
     */
    QueryResult createOrReusePatientSetQueryResult(String name, Constraint constraint, User user, String apiVersion)

    QueryResult findQueryResult(Long queryResultId, User user)

    Iterable<QueryResult> findPatientSetQueryResults(User user)

    Hypercube highDimension(
            Constraint assayConstraint_,
            BiomarkerConstraint biomarkerConstraint,
            String projectionName,
            User user,
            String type)

    Hypercube retrieveClinicalData(Constraint constraint, User user)

    Hypercube retrieveClinicalData(Constraint constraint, User user, List<Dimension> orderByDimensions)

    List<String> retrieveHighDimDataTypes(Constraint assayConstraint, User user)

    Iterable<Dimension> getSupportedDimensions(Constraint constraint)

}
