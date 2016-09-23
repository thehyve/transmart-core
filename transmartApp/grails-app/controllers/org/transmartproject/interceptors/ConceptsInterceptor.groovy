package org.transmartproject.interceptors

import org.transmartproject.core.users.User


class ConceptsInterceptor {

    def auditLogService
    def studyIdService
    User currentUserBean

    ConceptsInterceptor(){
        match(controller:'concepts', action: 'getChildren')
    }

    boolean before() {
            if (!auditLogService.enabled) return
            def studies = null
            if (params.concept_key) {
                studies = studyIdService.getStudyIdForConceptKey(params.concept_key, studyConceptOnly: true)
            }
            if (studies == null) return
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
