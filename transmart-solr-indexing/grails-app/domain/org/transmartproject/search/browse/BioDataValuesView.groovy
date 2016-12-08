package org.transmartproject.search.browse

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class BioDataValuesView implements Serializable {

    String uniqueId
    String name
    String description

    static mapping = {
        table schema: 'biomart_user'
        id composite: ['uniqueId']
        version false

        id insert: false, update: false
    }

    static constraints = {
        uniqueId    nullable: true, maxSize: 300
        name        nullable: true
        description nullable: true
    }
}
