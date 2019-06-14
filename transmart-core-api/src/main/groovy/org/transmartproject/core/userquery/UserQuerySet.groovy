package org.transmartproject.core.userquery

/**
 *  Stores query result changes
 *  @deprecated user queries related functionality has been moved to a gb-backend application
 */
@Deprecated
interface UserQuerySet {

    /**
     * Internal system identifier of the querySet.
     */
    Long getId()

    /**
     * Related query.
     */
    UserQuery getQuery()

    /**
     * The size of the set
     */
    Long getSetSize()

    /**
     * The type of the set the query is related to
     */
    SetType getSetType()

    /**
     * When the set was created.
     */
    Date getCreateDate()

    /**
     * List of objectIds that belong to the set
     */
    Set<UserQuerySetInstance> getQuerySetInstances()

    /**
     * List of objectIds added and/or removed from the set
     */
    Set<UserQuerySetDiff> getQuerySetDiffs()
}
