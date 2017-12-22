package org.transmartproject.core.userquery

/**
 *  Stores query result changes
 */
interface UserQueryDiff {

    /**
     * Internal system identifier of the queryDiff.
     */
    Long getId()

    /**
     * Identifier of the related query.
     */
    Long getQueryId()

    /**
     * Name of the related query.
     */
    String getQueryName()

    /**
     * Name of the related query.
     */
    String getQueryUsername()

    /**
     * The id of the set the query is related to
     */
    Long getSetId()

    /**
     * The type of the set the query is related to
     */
    String getSetType()

    /**
     * When the changes in result of this query were detected.
     */
    Date getDate()

    /**
     * List of objectIds added and/or removed from the set
     */
    List<UserQueryDiffEntry> getQueryDiffEntries()
}
