package org.transmartproject.db.user

class RoleCoreDb {

    public static final String ROLE_ADMIN_AUTHORITY = 'ROLE_ADMIN'

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
