package org.transmartproject.rest

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestContextHolder
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.ontology.Study

class StudyLoadingService {

    static scope = 'request'

    public static final String STUDY_ID_PARAM = 'studyId'

    def studiesResourceService

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

        studiesResourceService.getStudyByName(studyId)
    }
}
