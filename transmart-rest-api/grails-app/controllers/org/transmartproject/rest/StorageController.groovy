/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import grails.rest.RestfulController
import grails.transaction.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.users.AuthorisationChecks
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.storage.LinkedFileCollection
import org.transmartproject.rest.user.AuthContext

import javax.annotation.Resource

import static org.springframework.http.HttpStatus.CREATED
import static org.springframework.http.HttpStatus.OK

/**
 * Created by piotrzakrzewski on 02/12/2016.
 */
@Slf4j
class StorageController extends RestfulController<LinkedFileCollection> {

    static responseFormats = ['json']

    @Autowired
    AuthContext authContext

    @Autowired
    AuthorisationChecks authorisationChecks

    @Resource
    StudiesResource studiesResourceService

    StorageController() {
        super(LinkedFileCollection)
    }

    @Override
    @Transactional(readOnly = true)
    def show() {
        def fileCollection = queryForResource(params.getLong('id'))
        if (fileCollection == null) {
            notFound()
            return
        }
        if (!authorisationChecks.hasAnyAccess(authContext.user, fileCollection.study)) {
            throw new AccessDeniedException()
        }
        respond fileCollection
    }

    @Override
    @Transactional
    def save() {
        if (!authContext.user.admin) {
            throw new AccessDeniedException("Creating new Linked File Collections " +
                    "is an admin action")
        }
        def fields = request.JSON as Map
        def studyId = fields['study'] as String
        def study = Study.findByStudyId(studyId)
        fields['study'] = null
        def instance  = createResource fields
        instance.study = study
        instance.validate()
        if (instance.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond instance.errors, view:'create' // STATUS CODE 422
            return
        }
        saveResource instance
        log.info "Linked file collection saved with id: ${instance.id}"
        respond instance, [status: CREATED, view:'show']
    }

    @Override
    @Transactional(readOnly = true)
    def index() {
        if (!authContext.user.admin) {
            throw new AccessDeniedException("Listing all Linked File Collections " +
                    "is an admin action")
        }
        log.info "Fetching linked file collections."
        def response = ['files': listAllResources(params)]
        respond response
    }

    @Transactional(readOnly = true)
    def indexStudy(String studyId) {
        def study = Study.findByStudyId(studyId)
        if (!authorisationChecks.hasAnyAccess(authContext.user, study)) {
            throw new AccessDeniedException()
        }
        def filesInStudy = LinkedFileCollection.findAllByStudy(study)
        def response = ['files': filesInStudy  ]
        respond response
    }

    @Override
    //Adding @Transactional here causes stack overflow.
    def delete() {
        if (!authContext.user.admin) {
            throw new AccessDeniedException("Removing a linked file entry " +
                    "is an admin action")
        }
        super.delete()
    }

    @Override
    @Transactional
    def update() {
        if (!authContext.user.admin) {
            throw new AccessDeniedException("updating a linked file entry " +
                    "is an admin action")
        }
        def fields = request.JSON
        def studyId = fields['study'] as String
        def study = Study.findByStudyId(studyId)
        fields['study'] = study
        def instance = queryForResource(params.getLong('id'))
        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }
        instance.properties = fields

        if (instance.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond instance.errors, view:'edit' // STATUS CODE 422
            return
        }
        updateResource instance
        respond instance, [status: OK]
    }

}
