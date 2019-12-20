package org.transmartproject.interceptors

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Slf4j
class ApiAuditInterceptor extends AuditInterceptor {

    ApiAuditInterceptor() {
        match(controller: ~/arvados/)
        match(controller: ~/concept/)
        match(controller: ~/config/)
        match(controller: ~/dimension/)
        match(controller: ~/export/).excludes(action: ~/listJobs/)
        match(controller: ~/patientQuery/)
        match(controller: ~/relationType/)
        match(controller: ~/storage/)
        match(controller: ~/storageSystem/)
        match(controller: ~/query/)
        match(controller: ~/studyQuery/)
        match(controller: ~/system/)
        match(controller: ~/tree/)
        match(controller: ~/userQuery/)
        match(controller: ~/userQuerySet/)
        match(controller: ~/version/)
    }

    boolean before() {
        request.setAttribute(AUDIT_START_TIME_ATTRIBUTE, new Date())
        true
    }

    boolean after() {
        report("$controllerName request", eventMessage)
    }

}
