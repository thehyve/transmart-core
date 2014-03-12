package org.transmartproject.db.user

class RoleCoreDb {

    String authority
    String description

    static mapping = {
        table schema: 'searchapp', name: 'search_role'
    }

    static constraints = {
        authority   nullable: true, maxSize: 255
        description nullable: true, maxSize: 255
    }
}
