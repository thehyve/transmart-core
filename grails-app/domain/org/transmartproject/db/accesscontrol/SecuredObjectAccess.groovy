package org.transmartproject.db.accesscontrol

import org.transmartproject.db.user.Principal

class SecuredObjectAccess {

    static belongsTo = [
            principal:     Principal,
            securedObject: SecuredObject,
            accessLevel:   AccessLevel]

    static mapping = {
        table   schema: 'searchapp', name: 'search_auth_sec_object_access'

        id            column: 'auth_sec_obj_access_id', generator: 'assigned'
        principal     column: 'auth_principal_id'
        securedObject column: 'secure_object_id'
        accessLevel   column: 'secure_access_level_id'

        version false
    }

    static constraints = {
        principal nullable: true
    }
}
