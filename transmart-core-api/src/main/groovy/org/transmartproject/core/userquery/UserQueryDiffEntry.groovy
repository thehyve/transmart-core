package org.transmartproject.core.userquery

/**
 *  Stores query result change entries
 */
interface UserQueryDiffEntry {
    /**
     * Internal system identifier of the queryDiffEntry.
     */
    Long getId()

    /**
     * The id of the object that was updated in the query result
     */
    Long getObjectId()

    /**
     * Flag determining whether the object was added or removed
     */
    String getChangeFlag()

}
