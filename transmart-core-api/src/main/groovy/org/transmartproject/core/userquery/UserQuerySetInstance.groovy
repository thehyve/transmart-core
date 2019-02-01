package org.transmartproject.core.userquery

/**
 *  Stores query result change entries
 *  @deprecated user queries related functionality has been moved to a gb-backend application
 */
@Deprecated
interface UserQuerySetInstance {
    /**
     * Internal system identifier of the querySetInstance.
     */
    Long getId()

    /**
     * The id of the related object e.g. patient.id
     */
    Long getObjectId()

}
