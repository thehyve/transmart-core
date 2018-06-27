/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.ontology.MDStudiesResource
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.rest.marshallers.ContainerResponseWrapper
import org.transmartproject.rest.marshallers.StudyWrapper

import static org.transmartproject.rest.misc.RequestUtils.checkForUnsupportedParams
import static org.transmartproject.rest.misc.RequestUtils.parseJson

class StudyQueryController extends AbstractQueryController {

    static responseFormats = ['json', 'hal']

    @Autowired
    MDStudiesResource studiesResource

    /**
     * Studies endpoint:
     * <code>/v2/studies</code>
     *
     * @return a list of {@link org.transmartproject.db.i2b2data.Study} objects
     * that are accessible for the user.
     */
    def listStudies(@RequestParam('api_version') String apiVersion) {
        checkForUnsupportedParams(params, [])
        def studies = studiesResource.getStudies(authContext.user, PatientDataAccessLevel.minimalAccessLevel)
        respond wrapStudies(apiVersion, studies)
    }

    /**
     * Study endpoint:
     * <code>/v2/studies/${id}</code>
     *
     * @param id the study id
     *
     * @return the {@link org.transmartproject.db.i2b2data.Study} object with id ${id}
     * if it exists and the user has access; null otherwise.
     */
    def findStudy(
            @RequestParam('api_version') String apiVersion,
            @PathVariable('id') Long id) {
        if (id == null) {
            throw new InvalidArgumentsException("Parameter 'id' is missing.")
        }
        checkForUnsupportedParams(params, ['id'])

        def study = studiesResource.getStudyForUser(id, authContext.user)

        respond new StudyWrapper(
                study: study,
                apiVersion: apiVersion
        )
    }

    /**
     * Study endpoint:
     * <code>/v2/studies/studyId/${studyId}</code>
     *
     * @param id the study id
     *
     * @return the {@link org.transmartproject.db.i2b2data.Study} object with studyIds ${studyIds}
     * if it exists and the user has access; null otherwise.
     */
    def findStudyByStudyId(
            @RequestParam('api_version') String apiVersion,
            @PathVariable('studyId') String studyId) {
        if (studyId == null || studyId.trim().empty) {
            throw new InvalidArgumentsException("Parameter 'studyId' is missing.")
        }

        checkForUnsupportedParams(params, ['studyId'])

        def study = studiesResource.getStudyByStudyIdForUser(studyId, authContext.user)

        respond new StudyWrapper(
                study: study,
                apiVersion: apiVersion
        )
    }

    /**
     * Study endpoint:
     * <code>/v2/studies/studyIds?studyIds=[]</code>
     *
     * @param a json-encoded list of the study ids ${studyIds}
     *
     * @return a list of the {@link org.transmartproject.db.i2b2data.Study}
     * if all of them exist and the user has access; null otherwise.
     */
    def findStudiesByStudyIds(@RequestParam('api_version') String apiVersion) {
        if (params.studyIds == null) {
            throw new InvalidArgumentsException("Parameter 'studyIds' is missing.")
        }
        checkForUnsupportedParams(params, ['studyIds'])
        def studyIds = parseJson(params.studyIds) as List

        if (studyIds.size() == 0) {
            throw new InvalidArgumentsException("List of study ids is empty.")
        }

        def studies = studiesResource.getStudiesByStudyIdsForUser(studyIds, authContext.user)

        respond wrapStudies(apiVersion, studies)
    }

    private static wrapStudies(String apiVersion, Collection<MDStudy> source) {
        new ContainerResponseWrapper(
                key: 'studies',
                container: source.collect { new StudyWrapper(apiVersion: apiVersion, study: it) },
                componentType: MDStudy,
        )
    }

}
