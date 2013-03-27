package org.transmartproject.core.querytool

/**
 * A query result instance contains information about a specific run of a
 * query.
 *
 * At least for now, all query results are assumed to be of PATIENTSET type.
 */
interface QueryResult {

    /**
     * The query result instance id.
     *
     * @return the numerical identifier for this result
     */
    public Long getId()

    /**
     * The size of the set, or -1 if there was an error.
     *
     * @return the size of the set
     */
    public Long getSetSize()

    /**
     * The status of this query result instance.
     *
     * @return the status or null if unknown
     */
    public QueryStatus getStatus() //FINISHED (3), ERROR (4)

    /**
     * The error message associated with this query result. May be an
     * exception trace.
     *
     * @return the error message or null if none
     */
    public String getErrorMessage()

}
