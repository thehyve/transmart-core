package org.transmartproject.search.browse

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class BrowseFoldersView implements Serializable {

    String identifier
    String title
    String description
    String fileType

    static mapping = {
        table schema: 'biomart_user'
        id composite: ['identifier']
        version false

        id         insert: false, update: false
        identifier column: 'id'
    }

    static constraints = {
        identifier  nullable: true, maxSize: 300
        title       nullable: true, maxSize: 1000
        description nullable: true, maxSize: 2000
        fileType    nullable: true
    }
}
