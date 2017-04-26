package org.transmartproject.interceptors

import org.springframework.beans.factory.annotation.Autowired
import org.transmart.audit.StudyIdService
import org.transmartproject.core.audit.AuditLogger
import org.transmartproject.core.users.User


class ConceptsInterceptor {
    
    @Autowired(required = false)
    AuditLogger auditLogService
    @Autowired
    StudyIdService studyIdService
    @Autowired
    User currentUserBean

    ConceptsInterceptor(){
        match(controller:'concepts', action: 'getChildren')
    }

    boolean before() {
        if (!auditLogService.enabled) return true
        def studies = null
        if (params.concept_key) {
            studies = studyIdService.getStudyIdForConceptKey(params.concept_key, studyConceptOnly: true)
        }
        if (studies == null) return true
        def task = "Clinical Data Access"
        auditLogService.report(task, request,
                study: studies,
                user: currentUserBean
        )
        true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
