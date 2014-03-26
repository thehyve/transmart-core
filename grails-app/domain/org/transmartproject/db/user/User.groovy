package org.transmartproject.db.user

import grails.util.Holders
import org.hibernate.FetchMode
import org.hibernate.classic.Session
import org.transmartproject.core.ontology.Study
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.core.users.ProtectedResource
import org.transmartproject.db.accesscontrol.AccessLevel
import org.transmartproject.db.ontology.I2b2Secure

class User extends PrincipalCoreDb implements org.transmartproject.core.users.User {

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

    static mapping = {
        //table   schema: 'searchapp', name: 'search_auth_user'
        // ^^ Bug! doesn't work
        table   name: 'searchapp.search_auth_user'

        hash    column: 'passwd'

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

        // Since we only support access control on studies, let's implement
        // everything inline here
        // Later on, we should move this to some service
        if (!(protectedResource instanceof Study)) {
            throw new UnsupportedOperationException('Access control is ' +
                    'only supported on studies')
        }

        if (roles.find { it.authority == 'ROLE_ADMIN' }) {
            /* administrators bypass all the checks */
            log.debug "Bypassing check for $protectedOperation on " +
                    "$protectedResource for user $this because he is an " +
                    "administrator"
            return true
        }

        Study study = protectedResource

        /* Get the study's "token" */
        I2b2Secure secure =
                I2b2Secure.findByFullName study.ontologyTerm.fullName
        if (!secure) {
            log.warn "Could not find object '${study.ontologyTerm.fullName}' " +
                    "in i2b2_secure; allowing access"
            // must be true for backwards compatibility reasons
            // see I2b2HelperService::getAccess
            return true
        }

        String token = secure.secureObjectToken
        log.debug "Token for $study is $token"

        /* if token is EXP:PUBLIC, always permit */
        if (token == 'EXP:PUBLIC') {
            return true
        }

        /* see if the user has some access level for this */
        Session session = Holders.applicationContext.sessionFactory.currentSession
        def query = session.createQuery '''
            select soav.accessLevel from SecuredObjectAccessView soav
            where (soav.user = :user OR soav.user is null)
            and soav.securedObject.bioDataUniqueId = :token
            and soav.securedObject.dataType = 'BIO_CLINICAL_TRIAL'
            '''
        query.setParameter 'user', this
        query.setParameter 'token', token

        List<AccessLevel> results = query.list()
        log.debug("Got access levels for user $this, token $token: $results")

        if (!results) {
            log.info "No access level entries found for user $this and " +
                    "token $token; denying access"
            return false
        }

        if (results.any { protectedOperation in it }) {
            log.info("Access level of user $this for token $token " +
                    "granted through permission " +
                    "${results.find { protectedOperation in it }}")
            true
        } else {
            log.info("Permissions of user $this for token $token are " +
                    "only ${results as Set}; denying access")
            false
        }
    }
}
