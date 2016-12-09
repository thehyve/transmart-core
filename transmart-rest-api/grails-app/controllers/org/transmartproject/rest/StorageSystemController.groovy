package org.transmartproject.rest

import grails.rest.RestfulController
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.storage.StorageSystem
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


}
