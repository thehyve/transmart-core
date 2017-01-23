package org.transmartproject.core.log

/**
 * Holds information about arbitrary user action.
 */
interface AccessLogEntry {

    /**
     * @return internal db id (primary key value).
     */
    Long getId()

    /**
     * @return internal name for a user.
     */
    String getUsername()

    /**
     * @return short name for the event. e.g. data export
     */
    String getEvent()

    /**
     * @return message that contains some details about given event (could be null).
     */
    String getEventMessage()

    /**
     * @return url of the request that is associated with given event (could be null)
     */
    String getRequestURL()

    /**
     * @return time when event has happened
     */
    Date getAccessTime()

}
