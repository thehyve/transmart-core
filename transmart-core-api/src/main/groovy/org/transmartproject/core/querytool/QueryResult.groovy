package org.transmartproject.core.querytool

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.users.ProtectedResource

/**
 * A query result instance contains information about a specific run of a
 * query.
 *
 * All query results are assumed to be of PATIENTSET type.
 */
interface QueryResult extends ProtectedResource {

    /**
     * The query result instance id.
     *
     * @return the numerical identifier for this result
     */
    Long getId()

    /**
     * A description of the query, set by the creator.
     *
     * @return the textual description of the query.
     */
    String getDescription()

    /**
     * The size of the set, or -1 if there was an error.
     *
     * @return the size of the set
     */
    Long getSetSize()

    /**
     * The status of this query result instance.
     *
     * @return the status or null if unknown
     */
    QueryStatus getStatus() //FINISHED (3), ERROR (4)

    /**
     * The error message associated with this query result. May be an
     * exception trace.
     *
     * @return the error message or null if none
     */
    String getErrorMessage()

    /**
     * The set of patients included in this result.
     * @deprecated Query result is not necessarily associated with patients.
     * @return the set of patients
     */
    @Deprecated
    Set<Patient> getPatients()

    /**
     * The username of the user associated with this query. There may not
     * exist a user with this username anymore.
     *
     * @return the username associated with the query definition used to issue
     * this query
     */
    String getUsername()

    /**
     * Information on the type of the result query produced.
     * @return the query result type
     */
    QueryResultType getQueryResultType()

}
