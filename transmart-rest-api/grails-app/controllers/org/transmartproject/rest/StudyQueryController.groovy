package org.transmartproject.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.user.User
import org.transmartproject.rest.marshallers.ContainerResponseWrapper
import org.transmartproject.rest.marshallers.StudyWrapper

class StudyQueryController extends AbstractQueryController {

    static responseFormats = ['json', 'hal']

    @Autowired
    AccessControlChecks accessControlChecks

    /**
     * Studies endpoint:
     * <code>/v2/studies</code>
     *
     * @return a list of {@link org.transmartproject.db.i2b2data.Study} objects
     * that are accessible for the user.
     */
    def listStudies(@RequestParam('api_version') String apiVersion) {
        checkParams(params, [])
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def studies = accessControlChecks.getDimensionStudiesForUser(user)
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

        checkParams(params, ['id'])

        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def study = Study.findById(id)
        if (study == null || !user.canPerform(ProtectedOperation.WellKnownOperations.READ, study)) {
            throw new AccessDeniedException("Access denied to study or study does not exist: ${id}")
        }

        respond new StudyWrapper(
                study: study,
                apiVersion: apiVersion
        )
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
    def findStudyByStudyId(
            @RequestParam('api_version') String apiVersion,
            @PathVariable('studyId') String studyId) {
        if (studyId == null || studyId.trim().empty) {
            throw new InvalidArgumentsException("Parameter 'studyId' is missing.")
        }

        checkParams(params, ['studyId'])

        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def study = Study.findByStudyId(studyId)
        if (study == null || !user.canPerform(ProtectedOperation.WellKnownOperations.READ, study)) {
            throw new AccessDeniedException("Access denied to study or study does not exist: ${studyId}")
        }

        respond new StudyWrapper(
                study: study,
                apiVersion: apiVersion
        )
    }

    private def wrapStudies(String apiVersion, Collection<Study> source) {
        new ContainerResponseWrapper(
                key: 'studies',
                container: source.collect { new StudyWrapper(apiVersion: apiVersion, study: it) },
                componentType: Study,
        )
    }

}
