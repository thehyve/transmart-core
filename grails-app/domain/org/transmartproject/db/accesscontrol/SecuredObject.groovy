package org.transmartproject.db.accesscontrol

class SecuredObject {

    Long   bioDataId
    String displayName
    String dataType
    String bioDataUniqueId

    static mapping = {
        table schema: 'searchapp', name: 'search_secure_object'
        id column: 'search_secure_object_id', generator: 'assigned'
        version false
    }

    static constraints = {
        bioDataId       nullable: true
        displayName     nullable: true, maxSize: 100
        dataType        nullable: true, maxSize: 200
        bioDataUniqueId nullable: true, maxSize: 200
    }
}
