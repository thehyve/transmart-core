package org.transmartproject.core.log

import org.transmartproject.core.users.User

/**
 * Resource for registering and retrieving user actions.
 */
interface AccessLogEntryResource {

    /**
     * Save an user action
     * @param user whose action is reported
     * @param event a short descriptive name of an event.
     * @param additionalParams (optional):
     *  - eventMessage message that contains some details about given event
     *  - requestURL url of the request that is associated with given event
     *  - accessTime time when event has happened (now by default)
     * @return saved object with all reported details
     */
    AccessLogEntry report(Map<String, Object> additionalParams, User user, String event)

    /**
     * @param startDate used to filter out events (could be null)
     * @param endDate  used to filter out events (could be null)
     * @param paginationParams:
     *  - max number of entries to return
     *  - offset number of entries to skip
     *  - sort field name to sort on
     *  - order desc or asc
     * @return list of all events that meet data range constraint
     */
    List<AccessLogEntry> listEvents(Map<String, Object> paginationParams, Date startDate, Date endDate)

}
