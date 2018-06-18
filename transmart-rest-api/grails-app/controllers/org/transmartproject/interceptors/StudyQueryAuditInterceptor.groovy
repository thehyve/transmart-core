package org.transmartproject.interceptors

import groovy.transform.CompileStatic

@CompileStatic
class StudyQueryAuditInterceptor extends AuditInterceptor {

    StudyQueryAuditInterceptor() {
        match(controller: ~/studyQuery/, action: ~/listStudies|findStudy|findStudyByStudyId|findStudiesByStudyIds/)
    }

    boolean after() {
        report('Studies data retrieval', "User (IP: ${IP}) made a studies data request.")
    }

}
