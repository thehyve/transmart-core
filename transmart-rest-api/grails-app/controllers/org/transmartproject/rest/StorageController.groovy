package org.transmartproject.rest

import grails.rest.RestfulController
import groovy.util.logging.Slf4j

/**
 * Created by piotrzakrzewski on 02/12/2016.
 */
@Slf4j
class StorageController extends RestfulController {

    StorageController() {
        super(LinkedFileCollection)
    }
    static responseFormats = ['json']

    def

}
