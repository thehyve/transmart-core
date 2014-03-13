package org.transmartproject.rest

import grails.plugin.springsecurity.SpringSecurityUtils
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestContextHolder
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.ontology.Study
import org.transmartproject.core.users.ProtectedOperation

class StudyLoadingService {

    static transactional = false

    static scope = 'request'

    public static final String STUDY_ID_PARAM = 'studyId'

    def springSecurityService

    def studiesResourceService

    def usersResourceService

    private Study cachedStudy

    Study getStudy() {
        if (!cachedStudy) {
            cachedStudy = fetchStudy()
        }

        cachedStudy
    }

    Study fetchStudy() {
        GrailsWebRequest webRequest = RequestContextHolder.currentRequestAttributes()
        def studyId = webRequest.params.get STUDY_ID_PARAM

        if (!studyId) {
            throw new InvalidArgumentsException('Could not find a study id')
        }

        def study = studiesResourceService.getStudyByName(studyId)

        if (!checkAccess(study)) {
            throw new AccessDeniedException("Denied access to study ${study.name}")
        }

        study
    }

    private boolean checkAccess(Study study) {
        if (springSecurityService == null) {
            log.warn "Spring security service not available, " +
                    "granting access to study ${study.name} unconditionally"
            return true
        }

        if (!SpringSecurityUtils.securityConfig.active) {
            log.info "Spring security is inactive, " +
                    "granting access to study ${study.name} unconditionally"
            return true
        }

        if (!springSecurityService.isLoggedIn()) {
            log.warn "User is not logged in; denying access to study ${study.name}"
            return false
        }

        def username = springSecurityService.principal.username
        def user = usersResourceService.getUserFromUsername(username)

        def result = user.canPerform(
                ProtectedOperation.WellKnownOperations.API_READ, study)
        if (!result) {
            log.warn "User $username denied access to study ${study.name}"
        }

        result
    }

    String getStudyLowercase() {
        study.name.toLowerCase(Locale.ENGLISH)
    }
}
