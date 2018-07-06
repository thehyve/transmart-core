/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.log

import grails.transaction.Transactional
import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.transmartproject.core.binding.BindingHelper
import org.transmartproject.core.log.AccessLogEntryResource
import org.transmartproject.core.users.User

@Transactional
@CompileStatic
class AccessLogService implements AccessLogEntryResource {

    static final Logger log = LoggerFactory.getLogger(AccessLogService.class)

    @Override
    AccessLogEntry report(Map<String, Object> additionalParams = [:], User user, String event) {
        def eventMessage = additionalParams?.eventMessage as String
        def requestURL = additionalParams?.requestURL as String
        def accessTime = additionalParams?.accessTime as Date ?: new Date()
        log.trace(BindingHelper.objectMapper.writeValueAsString([
                username:       user?.username,
                event:          event,
                eventMessage:   eventMessage,
                requestURL:     requestURL,
                accessTime:     accessTime]))
        new AccessLogEntry(
                username:       user?.username,
                event:          event.take(255),
                eventMessage:   eventMessage,
                requestURL:     requestURL?.take(255),
                accessTime:     accessTime,
        ).save()
    }

    @Override
    List<AccessLogEntry> listEvents(Map<String, Object> paginationParams = [:], Date startDate, Date endDate) {
        AccessLogEntry.createCriteria().list(
                max:    paginationParams?.max,
                offset: paginationParams?.offset,
                sort:   paginationParams?.sort,
                order:  paginationParams?.order) {

            if (startDate && endDate) {
                between 'accessTime', startDate, endDate
            } else if (startDate) {
                gte 'accessTime', startDate
            } else if (endDate) {
                lte 'accessTime', endDate
            }
        } as List<AccessLogEntry>
    }

}
