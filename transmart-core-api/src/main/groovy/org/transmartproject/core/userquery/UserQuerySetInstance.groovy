package org.transmartproject.core.userquery

/**
 *  Stores query result change entries
 */
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
