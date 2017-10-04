package org.transmartproject.core.querytool

/**
 * Represent a type of the query result. e.g. patient set or generic query result.
 */
interface QueryResultType {

    static final long PATIENT_SET_ID = 1
    static final long GENERIC_QUERY_RESULT_ID = 3

    /**
     * The query result type id.
     *
     * @return the numerical identifier for this result
     */
    Long getId()

    /**
     * A description of the query type.
     *
     * @return the textual description of the query.
     */
    String getDescription()

}
