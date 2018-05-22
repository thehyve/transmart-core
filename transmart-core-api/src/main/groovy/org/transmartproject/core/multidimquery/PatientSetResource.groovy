package org.transmartproject.core.multidimquery

import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.User

interface PatientSetResource {

    /**
     * Function for creating a patient set consisting of patients for which there are observations
     * that are specified by <code>query</code>.
     *
     * @param name a name for the patient set
     * @param constraint the constraint used for querying patients
     * @param user the current user
     * @param apiVersion the API version used at time of patient set creation
     * @param reusePatientSet whether to allow the implementation to return an existing patient set for the same query.
     */
    QueryResult createPatientSetQueryResult(String name, Constraint constraint, User user, String apiVersion, boolean reusePatientSet)

    /**
     * Find a query result based on a result instance id.
     *
     * @param queryResultId the result instance id of the query result.
     * @param user the creator of the query result.
     * @return the query result if it exists.
     * @throws org.transmartproject.core.exceptions.NoSuchResourceException iff the query result does not exist.
     */
    QueryResult findQueryResult(Long queryResultId, User user)

    /**
     * Find a query result based on a constraint.
     *
     * @param user the creator of the query result.
     * @param constraint the constraint used in the lookup.
     * @return the query result if it exists; null otherwise.
     */
    QueryResult findQueryResultByConstraint(User user, Constraint constraint)

    /**
     * Retrieves all query results for a user.
     *
     * @param user the user to retrieve query results for.
     * @return the query result objects.
     */
    Iterable<QueryResult> findPatientSetQueryResults(User user)

}