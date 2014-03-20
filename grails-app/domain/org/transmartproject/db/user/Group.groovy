package org.transmartproject.db.user

class Group extends PrincipalCoreDb {

    String category // difference to name or uniqueId in PrincipalCoreDb?

    static mapping = {
        //table schema: 'searchapp', name: 'search_auth_group'
        // ^^ Bug! doesn't work
        table name: 'searchapp.search_auth_group'

        category column: 'group_category'

        discriminator name: 'GROUP', column: 'unique_id'

        version false
    }

    static constraints = {
        category nullable: true
    }
}
