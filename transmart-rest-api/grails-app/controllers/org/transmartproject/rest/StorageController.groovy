package org.transmartproject.rest

import grails.rest.RestfulController
import groovy.util.logging.Slf4j
import org.transmartproject.db.storage.LinkedFileCollection

/**
 * Created by piotrzakrzewski on 02/12/2016.
 */
@Slf4j
class StorageController extends RestfulController {

    static responseFormats = ['json']

    StorageController() {
        super(LinkedFileCollection)
    }


    //def

}
