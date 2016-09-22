package org.transmartproject.interceptors

import org.transmartproject.core.users.User

class ChartInterceptor {

    def auditLogService
    def studyIdService
    User currentUserBean

    ChartInterceptor(){
        match(controller: 'chart').excludes(action:~/(clearGrid|displayChart|childConceptPatientCounts)/)
    }

    boolean before() {
        if (!auditLogService.enabled) return true
        def result_instance_id1 = params.result_instance_id1 ?: ''
        def result_instance_id2 = params.result_instance_id2 ?: ''
        def studies = ''
        if (params.concept_key) {
            studies = studyIdService.getStudyIdForConceptKey(params.concept_key) ?: ''
        }
        if (studies.empty) {
            studies = studyIdService.getStudyIdsForQueries([result_instance_id1, result_instance_id2])
        }
        def task = "Summary Statistics"
        if (actionName == "childConceptPatientCounts") {
            task = "Clinical Data Access"
        }
        auditLogService.report(task, request,
                study: studies,
                user: currentUserBean,
                subset1: result_instance_id1,
                subset2: result_instance_id2
        )
        true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
