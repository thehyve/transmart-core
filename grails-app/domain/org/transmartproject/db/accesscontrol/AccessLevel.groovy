package org.transmartproject.db.accesscontrol

import org.transmartproject.core.users.Permission

class AccessLevel implements Permission {

    String name
    Long   value

    static hasMany = [searchAuthSecObjectAccesses: SecuredObjectAccess]

    static mapping = {
        table schema: 'searchapp', name: 'search_sec_access_level'

        id    column: 'search_sec_access_level_id', generator: 'assigned'
        name  column: 'access_level_name'
        value column: 'access_level_value'

        version false
    }

    static constraints = {
        name  nullable: true, maxSize: 200
        value nullable: true
    }


    @Override
    public String toString() {
        com.google.common.base.Objects.toStringHelper(this)
                .add("name", name)
                .add("value", value).toString()
    }
}
