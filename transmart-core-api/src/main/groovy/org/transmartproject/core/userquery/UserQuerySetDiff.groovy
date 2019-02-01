package org.transmartproject.core.userquery

/**
 *  Stores query result change entries
 *  @deprecated user queries related functionality has been moved to a gb-backend application
 */
@Deprecated
interface UserQuerySetDiff {
    /**
     * Internal system identifier of the querySetInstance.
     */
    Long getId()

    /**
     * The id of the object that was updated in the query set e.g. patient.id
     */
    Long getObjectId()

    /**
    *  Flag determining whether the object was added or removed
    */
    ChangeFlag getChangeFlag()

}
