package org.transmartproject.core.querytool

import org.transmartproject.core.users.ProtectedResource

/**
 * Created by ewelina on 30-8-16.
 */
interface QueryResultSummary extends ProtectedResource {

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

    /**
     * The username of the user associated with this query. There may not
     * exist a user with this username anymore.
     *
     * @return the username associated with the query definition used to issue
     * this query
     */
    public String getUsername()
}
