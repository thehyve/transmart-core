package org.transmartproject.db.user

class Principal {

    String  principalType
    String  description
    String  name
    String  uniqueId
    Boolean enabled

    static mapping = {
        table   schema:    'searchapp', name: 'search_auth_principal'
        id      generator: 'assigned'

        autoTimestamp true
        version       false

        tablePerSubclass true
    }

    static constraints = {
        principalType nullable: true, maxSize: 255
        description   nullable: true, maxSize: 255
        name          nullable: true, maxSize: 255
        uniqueId      nullable: true, maxSize: 255
        enabled       nullable: true
    }
}
