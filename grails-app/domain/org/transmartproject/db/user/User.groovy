package org.transmartproject.db.user

import org.hibernate.FetchMode
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.core.users.ProtectedResource
import org.transmartproject.db.accesscontrol.AccessControlChecks

class User extends PrincipalCoreDb implements org.transmartproject.core.users.User {

    @Autowired
    AccessControlChecks accessControlChecks

    String  email
    Boolean emailShow
    String  hash
    String  realName
    String  username

    /* not mapped (only on thehyve/master) */
    //String federatedId

    static hasMany = [
            roles:  RoleCoreDb,
            groups: Group
    ]

    static transients = ['accessControlChecks']

    static mapping = {
        //table   schema: 'searchapp', name: 'search_auth_user'
        // ^^ Bug! doesn't work
        table   name: 'searchapp.search_auth_user'

        hash    column: 'passwd'

        // no way to fetch the roles' properties themselves :(
        // http://stackoverflow.com/questions/4208728
        roles   joinTable: [//name:   'search_role_auth_user',
                            name:   'searchapp.search_role_auth_user',
                            key:    'authorities_id',
                            column: 'people_id'], // insane column naming!
                fetch: FetchMode.JOIN

        groups  joinTable: [//name:   'search_auth_group_member',
                            name:   'searchapp.search_auth_group_member',
                            key:    'auth_user_id',
                            column: 'auth_group_id']

        discriminator name: 'USER', column: 'unique_id'

        cache   usage: 'read-only', include: 'non-lazy' /* don't cache groups */

        realName column: 'user_real_name'

        version false
    }

    static constraints = {
        email        nullable: true, maxSize: 255
        emailShow    nullable: true
        hash         nullable: true, maxSize: 255
        realName     nullable: true, maxSize: 255
        username     nullable: true, maxSize: 255
        //federatedId nullable: true, unique: true
    }

    @Override
    boolean canPerform(ProtectedOperation protectedOperation,
                       ProtectedResource protectedResource) {

        if (!accessControlChecks.respondsTo('canPerform',
                [User, ProtectedOperation, protectedResource.getClass()] as Object[])) {
            throw new UnsupportedOperationException("Do not know how to check " +
                    "access for user $this, operation $protectedOperation on " +
                    "$protectedResource")
        }

        if (roles.find { it.authority == RoleCoreDb.ROLE_ADMIN_AUTHORITY }) {
            /* administrators bypass all the checks */
            log.debug "Bypassing check for $protectedOperation on " +
                    "$protectedResource for user $this because he is an " +
                    "administrator"
            return true
        }

        accessControlChecks.canPerform(this,
                                       protectedOperation,
                                       protectedResource)
    }
}
