package org.transmartproject.core.userquery

/**
 *  Stores patients and observations parts of queries
 */
interface UserQuery {
    /**
     * Internal system identifier of the query.
     */
    Long getId()

    /**
     * The query name
     */
    String getName()

    void setName(String name)

    /**
     * The patient selection part of the query in json format.
     */
    String getPatientsQuery()

    void setPatientsQuery(String patientsQuery)

    /**
     * The observation selection part of the query.
     */
    String getObservationsQuery()

    void setObservationsQuery(String observationsQuery)

    /**
     * The version of the API the query was intended for.
     */
    String getApiVersion()

    void setApiVersion(String apiVersion)

    /**
     * Flag to indicate if the user has bookmarked the query.
     */
    Boolean getBookmarked()

    void setBookmarked(Boolean bookmarked)

    /**
     * Creation date and time of this query.
     */
    Date getCreateDate()

    /**
     * When this query was updated.
     */
    Date getUpdateDate()
}
