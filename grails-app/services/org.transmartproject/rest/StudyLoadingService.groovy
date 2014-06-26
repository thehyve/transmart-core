/*
 * Copyright 2014 Janssen Research & Development, LLC.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.rest

import grails.plugin.springsecurity.SpringSecurityUtils
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestContextHolder
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.Study
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.rest.ontology.OntologyTermCategory

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

        def study = studiesResourceService.getStudyById(studyId)

        if (!checkAccess(study)) {
            throw new AccessDeniedException("Denied access to study ${study.id}")
        }

        study
    }

    private boolean checkAccess(Study study) {
        if (springSecurityService == null) {
            log.warn "Spring security service not available, " +
                    "granting access to study ${study.id} unconditionally"
            return true
        }

        if (!SpringSecurityUtils.securityConfig.active) {
            log.info "Spring security is inactive, " +
                    "granting access to study ${study.id} unconditionally"
            return true
        }

        if (!springSecurityService.isLoggedIn()) {
            log.warn "User is not logged in; denying access to study ${study.id}"
            return false
        }

        def username = springSecurityService.principal.username
        def user = usersResourceService.getUserFromUsername(username)

        def result = user.canPerform(
                ProtectedOperation.WellKnownOperations.API_READ, study)
        if (!result) {
            log.warn "User $username denied access to study ${study.id}"
        }

        result
    }

    String getStudyLowercase() {
        study.id.toLowerCase(Locale.ENGLISH)
    }

    /**
     * @param term ontology term
     * @return url for given ontology term and current study
     */
    String getOntologyTermUrl(OntologyTerm term) {
        //this gets tricky. We may be rendering this as part of the /studies response
        String studyId
        def pathPart

        try {
            studyId = study.id
            use (OntologyTermCategory) {
                pathPart = term.encodeAsURLPart study
            }
        } catch (InvalidArgumentsException iae) {
            /* studyId not in params; so we are handling a study, which
             * is mapped to $id (can we rename the param to $studyId
             * for consistency?)
             */
            studyId = term.study.id
            pathPart = 'ROOT'
        }
        studyId = studyId.toLowerCase(Locale.ENGLISH).encodeAsURL()
        "/studies/$studyId/concepts/$pathPart"
    }

}
