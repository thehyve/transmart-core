package org.transmartproject.interceptors

import groovy.transform.CompileStatic

@CompileStatic
class StudyQueryAuditInterceptor extends AuditInterceptor {

    StudyQueryAuditInterceptor() {
        match(controller: ~/studyQuery/, action: ~/listStudies|findStudy|findStudyByStudyId|findStudiesByStudyIds/)
    }

    boolean after() {
        def ip = getIP()
        report('Studies data retrieval', "User (IP: ${ip}) made a studies data request.")
    }

}
