package org.transmartproject.solr

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class BioDataValuesView implements Serializable {

    String id
    String name
    String description

    static mapping = {
        table schema: 'biomart_user'
        version false

        id column: 'unique_id' , insert: false, update: false
    }

    static constraints = {
        id    nullable: true, maxSize: 300
        name        nullable: true
        description nullable: true
    }

    def getUniqueId() {
        id
    }
}
