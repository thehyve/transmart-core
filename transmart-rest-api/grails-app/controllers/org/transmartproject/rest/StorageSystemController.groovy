package org.transmartproject.rest

import grails.rest.RestfulController
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.storage.StorageSystem
import org.transmartproject.db.user.User
import org.transmartproject.rest.misc.CurrentUser

/**
 * Created by piotrzakrzewski on 09/12/2016.
 */
class StorageSystemController extends RestfulController {

    StorageSystemController() {
        super(StorageSystem)
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
            throw new AccessDeniedException("Creating new storage system entry" +
                    "is an admin action")
        }
        super.save()
    }

    @Override
    def delete() {
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        if (!user.admin) {
            throw new AccessDeniedException("Removing a storage system entry " +
                    "is an admin action")
        }
        super.delete()
    }

    @Override
    def update() {
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        if (!user.admin) {
            throw new AccessDeniedException("Removing a storage system entry " +
                    "is an admin action")
        }
        super.delete()
    }

    @Override
    def index() {
        def response = ['storageSystems': listAllResources(params)]
        respond response
    }


}
