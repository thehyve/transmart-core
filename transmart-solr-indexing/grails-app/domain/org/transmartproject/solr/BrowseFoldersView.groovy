package org.transmartproject.solr

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class BrowseFoldersView implements Serializable {

    String id
    String title
    String description
    String fileType

    static mapping = {
        table schema: 'biomart_user'
        version false

        id         insert: false, update: false
    }

    static constraints = {
        id  nullable: true, maxSize: 300
        title       nullable: true, maxSize: 1000
        description nullable: true, maxSize: 2000
        fileType    nullable: true
    }

    def getIdentifier() {
        id
    }
}
