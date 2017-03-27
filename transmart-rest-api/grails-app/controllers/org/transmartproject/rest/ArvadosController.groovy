/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import grails.converters.JSON
import grails.rest.RestfulController
import grails.web.http.HttpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.arvados.SupportedWorkflow
import org.transmartproject.db.user.User
import org.transmartproject.rest.misc.CurrentUser

import static org.springframework.http.HttpStatus.CREATED

/**
 * Created by piotrzakrzewski on 15/12/2016.
 */
class ArvadosController extends RestfulController {

    ArvadosController() {
        super(SupportedWorkflow)
    }

    @Autowired
    UsersResource usersResource

    static responseFormats = ['json']

    @Autowired
    CurrentUser currentUser

    @Override
    def save() {
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        if (!user.admin) {
            throw new AccessDeniedException("Creating a new supported workflow" +
                    "is an admin action")
        }
        def instance = resource.newInstance()
        def fields = request.JSON
        if (fields['defaultParams']){
            def defaultParams = fields['defaultParams'] as JSON
            defaultParams = defaultParams.toString()
            fields['defaultParams'] = defaultParams
        } else {
            fields['defaultParams'] = "{}"
        }
        bindData instance, fields

        instance.validate()
        if (instance.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond instance.errors, view:'create' // STATUS CODE 422
            return
        }
        saveResource instance

        respond instance, [status: CREATED, view:'show']
    }

    @Override
    def delete() {
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        if (!user.admin) {
            throw new AccessDeniedException("Removing a new supported workflow entry " +
                    "is an admin action")
        }
        super.delete()
    }

    @Override
    def update() {
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        if (!user.admin) {
            throw new AccessDeniedException("Modifying a supported workflow entry " +
                    "is an admin action")
        }
        super.update()
    }

    @Override
    def index() {
        def response = ['supportedWorkflows': listAllResources(params)]
        respond response
    }


}
