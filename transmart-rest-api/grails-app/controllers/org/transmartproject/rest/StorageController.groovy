package org.transmartproject.rest

import grails.rest.RestfulController
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.storage.LinkedFileCollection
import org.transmartproject.db.user.User
import org.transmartproject.rest.misc.CurrentUser

import javax.annotation.Resource

import static org.transmartproject.core.users.ProtectedOperation.WellKnownOperations.READ

/**
 * Created by piotrzakrzewski on 02/12/2016.
 */
@Slf4j
class StorageController extends RestfulController {

    static responseFormats = ['json']

    @Autowired
    CurrentUser currentUser

    @Resource
    StudiesResource studiesResourceService

    @Autowired
    UsersResource usersResource

    StorageController() {
        super(LinkedFileCollection)
    }

    @Override
    def show() {
        def fileCollection = queryForResource(params.id)
        def studyId = fileCollection.study
        def study = studiesResourceService.getStudyById(studyId)
        currentUser.checkAccess(READ, study)
        respond fileCollection
    }

    @Override
    def index(Integer max) {
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        if (!user.admin) {
            throw new AccessDeniedException("Listing all Linked File Collections " +
                    "is an admin action")
        }
        params.max = Math.min(max ?: 100, 1000) // hard limit on number of results, we can consider removing
        def response = ['files':listAllResources(params)]
        respond response
    }

    def indexStudy(String studyId, Integer max) {
        def study = studiesResourceService.getStudyById(studyId)
        currentUser.checkAccess(READ, study)
        params.max = Math.min(max ?: 100, 1000) // hard limit on number of results, we can consider removing
        respond listAllResources(params), model: [("${resourceName}Count".toString()): countResources()]
    }

}
