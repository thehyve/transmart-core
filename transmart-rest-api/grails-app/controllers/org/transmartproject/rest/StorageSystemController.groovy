/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import grails.rest.RestfulController
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.db.storage.StorageSystem
import org.transmartproject.rest.user.AuthContext

/**
 * Created by piotrzakrzewski on 09/12/2016.
 */
class StorageSystemController extends RestfulController<StorageSystem> {

    StorageSystemController() {
        super(StorageSystem)
    }

    static responseFormats = ['json']

    @Autowired
    AuthContext authContext

    @Override
    def save() {
        if (!authContext.user.admin) {
            throw new AccessDeniedException("Creating new storage system entry" +
                    "is an admin action")
        }
        super.save()
    }

    @Override
    def delete() {
        if (!authContext.user.admin) {
            throw new AccessDeniedException("Removing a storage system entry " +
                    "is an admin action")
        }
        super.delete()
    }

    @Override
    def update() {
        if (!authContext.user.admin) {
            throw new AccessDeniedException("Removing a storage system entry " +
                    "is an admin action")
        }
        super.update()
    }

    @Override
    def index() {
        def response = ['storageSystems': listAllResources(params)]
        respond response
    }


}
