package org.transmartproject.mock

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.transmartproject.core.binding.BindingHelper
import org.transmartproject.core.log.AccessLogEntry
import org.transmartproject.core.log.AccessLogEntryResource
import org.transmartproject.core.users.User

@Slf4j
@CompileStatic
class MockAccessLogEntryResource implements AccessLogEntryResource {

    List<AccessLogEntry> entries = []

    @Override
    AccessLogEntry report(Map<String, Object> additionalParams, User user, String event) {
        def eventMessage = additionalParams?.eventMessage as String
        def requestURL = additionalParams?.requestURL as String
        def accessTime = additionalParams?.accessTime as Date ?: new Date()
        def entry = new MockAccessLogEntry(
                null,
                user.username,
                event,
                eventMessage,
                requestURL,
                accessTime
        )
        log.info(BindingHelper.objectMapper.writeValueAsString([
                username:       user.username,
                event:          event,
                eventMessage:   eventMessage,
                requestURL:     requestURL,
                accessTime:     accessTime]))
        entries << entry
        entry
    }

    @Override
    List<AccessLogEntry> listEvents(Map<String, Object> paginationParams, Date startDate, Date endDate) {
        null
    }

}
