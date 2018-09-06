package org.transmartproject.rest.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.contrib.json.JsonLayoutBase
import groovy.transform.CompileStatic
import org.transmartproject.core.binding.BindingHelper

/**
 * Custom Json layout for writing audit metrics of API calls.
 */
@CompileStatic
class ApiAuditLogJsonLayout extends JsonLayoutBase<ILoggingEvent> {

    public static final String USERNAME_ATTR_NAME = "username"
    public static final String EVENT_ATTR_NAME = "event"
    public static final String EVENT_MESSAGE_ATTR_NAME = "eventMessage"
    public static final String REQUEST_URL_ATTR_NAME = "requestURL"
    public static final String ACCESS_TIME_ATTR_NAME = "accessTime"

    @Override
    protected Map toJsonMap(ILoggingEvent event) {
        Map<String, Object> map = new LinkedHashMap<String, Object>()

        InputStream stream = new ByteArrayInputStream(event.getFormattedMessage().getBytes())
        def message = BindingHelper.read(stream, ApiAuditLogRepresentation)

        add(USERNAME_ATTR_NAME, true, String.valueOf(message.username), map)
        add(EVENT_ATTR_NAME, true, String.valueOf(message.event), map)
        add(EVENT_MESSAGE_ATTR_NAME, true, String.valueOf(message.eventMessage), map)
        add(REQUEST_URL_ATTR_NAME, true, String.valueOf(message.requestURL), map)
        add(ACCESS_TIME_ATTR_NAME, true, String.valueOf(message.accessTime), map)

        return map
    }

}
