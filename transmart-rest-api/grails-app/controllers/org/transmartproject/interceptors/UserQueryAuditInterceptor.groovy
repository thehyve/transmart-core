package org.transmartproject.interceptors

import groovy.transform.CompileStatic

@CompileStatic
class UserQueryAuditInterceptor extends AuditInterceptor {

    UserQueryAuditInterceptor() {
        match(controller: ~/(userQuery|userQuerySet)/,
                action: ~/index|get|save|update|delete|scan|getSetChangesByQueryId/)
    }

    boolean after() {
        report("User query related call.", "User (IP: $IP) made a user query related call. Action: $actionName.")
    }

}
