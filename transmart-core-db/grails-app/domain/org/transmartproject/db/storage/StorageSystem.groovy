/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.db.storage

/**
 * Created by piotrzakrzewski on 02/12/2016.
 */
class StorageSystem {
    String name       // e.g. "dev db for demo server"
    String systemType // e.g. MongoDB, Arvados
    String url        // resolvable url to the storage system
    String systemVersion    // version of the storage system,
    // called systemVersion because version is a resevered column in grails
    boolean singleFileCollections // true for systems where FileCollection=file

    static mapping = {
        table          schema:   'I2B2DEMODATA'
        singleFileCollections defaultValue: false
        version false
        id generator: 'sequence'
    }

    static constraints = {
    }

}
