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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.Study
import org.transmartproject.core.users.LegacyAuthorisationChecks
import org.transmartproject.rest.user.AuthContext
import org.transmartproject.rest.ontology.OntologyTermCategory

/**
 * Service that retrieves the current study based on the study id in the request path.
 *
 * @deprecated Study ids should be passed explicitly to controllers and services when required,
 * instead of prefixing every request with a study path.
 */
@Deprecated
class StudyLoadingService {

    static scope = 'request'

    public static final String STUDY_ID_PARAM = 'studyId'

    def studiesResourceService

    @Autowired
    AuthContext authContext

    @Autowired
    LegacyAuthorisationChecks authorisationChecks

    private Study cachedStudy

    Study getStudy() throws NoSuchResourceException {
        if (!cachedStudy) {
            cachedStudy = fetchStudy()
        }

        cachedStudy
    }

    Study fetchStudy() throws NoSuchResourceException {
        RequestAttributes webRequest = RequestContextHolder.currentRequestAttributes()
        def studyId = webRequest.params.get STUDY_ID_PARAM

        if (!studyId) {
            throw new InvalidArgumentsException('Could not find a study id')
        }

        def study = studiesResourceService.getStudyById(studyId)

        if (!authorisationChecks.hasAccess(authContext.user, study)) {
            throw new AccessDeniedException("Denied access to study ${study.id}")
        }

        study
    }

    String getStudyLowercase() {
        study.id.toLowerCase(Locale.ENGLISH)
    }

    /**
     * @param term ontology term
     * @return url for given ontology term and request study or concept study
     */
    String getOntologyTermUrl(OntologyTerm term) {
        String studyId
        def pathPart

        try {
            studyId = study.id
            use (OntologyTermCategory) {
                pathPart = term.encodeAsURLPart study
            }
        } catch (InvalidArgumentsException iae) {
            //studyId not in params: either /studies or a study controller
            studyId = term.study.id
            if (term.level == 1) {
                //we are handling a study, which is mapped to $id (can we rename the param to $studyId for consistency?)
                pathPart = 'ROOT'
            } else {
                use (OntologyTermCategory) {
                    pathPart = term.encodeAsURLPart term.study
                }
            }
        }
        studyId = studyId.toLowerCase(Locale.ENGLISH).encodeAsURL()
        "/v1/studies/$studyId/concepts/$pathPart"
    }

}
