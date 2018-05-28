package org.transmartproject.interceptors

import groovy.transform.CompileStatic

@CompileStatic
class UserQueryAuditInterceptor extends AuditInterceptor {

    UserQueryAuditInterceptor() {
        match(controller: ~/(userQuery|userQuerySet)/,
                action: ~/index|get|save|update|delete|scan|getSetChangesByQueryId/)
    }

    boolean after() {
        def ip = getIP()
        Map<String, String> event = getEventMessage(ip)
        report(event.event, event.eventMessage)
    }

    private Map<String, String> getEventMessage(ip) {
        switch (actionName) {
            case 'delete':
                return [event       : "User query deletion",
                        eventMessage: "User (IP: ${ip}) deleted a user query."] as Map<String, String>
            case 'save':
                return [event       : "User query creation",
                        eventMessage: "User (IP: ${ip}) created a new user query."] as Map<String, String>
            case 'update':
                return [event       : "User query update",
                        eventMessage: "User (IP: ${ip}) updated a user query."] as Map<String, String>
            case 'scan':
                return [event       : "User query changes scan",
                        eventMessage: "User (IP: ${ip}) started a scan for changes in results " +
                                "of the stored queries."] as Map<String, String>
            default:
                return [event       : "User query retrieval",
                        eventMessage: "User (IP: ${ip}) made a user query request."] as Map<String, String>
        }
    }
}
