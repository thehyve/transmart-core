package org.transmartproject.db.accesscontrol

import grails.util.Holders
import groovy.util.logging.Log4j
import org.hibernate.classic.Session
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.ontology.Study
import org.transmartproject.core.querytool.Item
import org.transmartproject.core.querytool.Panel
import org.transmartproject.core.querytool.QueryDefinition
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.db.ontology.I2b2Secure
import org.transmartproject.db.user.User

/**
 * Access control methods for use in {@link org.transmartproject.db.user.User}.
 *
 * Right now these are implemented in this same class.
 * If this list gets bigger or needs to be pluggable by other plugins, then
 * this should be refactored into a different solution (e.g. each check
 * implemented in a different Spring bean).
 */
@Log4j
@Component
class AccessControlChecks {

    @Autowired
    ConceptsResource conceptsResource

    @Autowired
    StudiesResource studiesResource

    boolean canPerform(User user,
                       ProtectedOperation protectedOperation,
                       Study study) {

        if (user.roles.find { it.authority == 'ROLE_ADMIN' }) {
            /* administrators bypass all the checks */
            log.debug "Bypassing check for $protectedOperation on " +
                    "$study for user $this because he is an " +
                    "administrator"
            return true
        }

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
        query.setParameter 'user', user
        query.setParameter 'token', token

        List<AccessLevel> results = query.list()
        log.debug("Got access levels for user $this, token $token: $results")

        if (!results) {
            log.info "No access level entries found for user $this and " +
                    "token $token; denying access"
            return false
        }

        if (results.any { protectedOperation in it }) {
            log.debug("Access level of user $this for token $token " +
                    "granted through permission " +
                    "${results.find { protectedOperation in it }}")
            true
        } else {
            log.info("Permissions of user $this for token $token are " +
                    "only ${results as Set}; denying access")
            false
        }
    }

    boolean canPerform(User user,
                       ProtectedOperation operation,
                       QueryDefinition definition) {
        if (operation != ProtectedOperation.WellKnownOperations.BUILD_COHORT) {
            log.warn "Requested protected operation different from " +
                    "BUILD_COHORT on QueryDefinition $definition"
            throw new UnsupportedOperationException("Operation $operation ")
        }

        // check there is at least one non-inverted panel for which the user
        // has permission in all the terms
        def res = definition.panels.findAll { !it.invert }.any { Panel panel ->
            Set<Study> foundStudies = panel.items.collect { Item item ->

                /* this could be optimized by adding a new method in
                 * StudiesResource */
                def concept = conceptsResource.getByKey(item.conceptKey)
                def study = concept.study

                if (study == null) {
                    log.info "User included concept with no study: $concept"
                }

                study
            } as Set

            foundStudies.every { Study study1 ->
                if (study1 == null) {
                    return false
                }
                canPerform user, operation, study1
            }
        }

        if (!res) {
            log.warn "User $user defined access for definition $definition " +
                    "because it doesn't include one non-inverted panel for" +
                    "which the user has permission in all the terms' studies"
        } else {
            log.debug "Granting access to user $user to use " +
                    "query definition $definition"
        }

        res
    }

    boolean canPerform(User user,
                       ProtectedOperation operation,
                       QueryResult result) {
        if (operation != ProtectedOperation.WellKnownOperations.READ) {
            log.warn "Requested protected operation different from " +
                    "READ on QueryResult $result"
            throw new UnsupportedOperationException("Operation $operation ")
        }

        /* Note that this check doesn't account for the fact that the user's
         * permissions on the check from which the result was generated may
         * have been revoked in the meantime. We could check again the access
         * to the studies with:
         *
         * def qd = queryDefinitionXml.fromXml(new StringReader(
         *         queryResult.queryInstance.queryMaster.requestXml))
         * canPerform(user, BUILD_COHORT, qd)
         *
         * However, this would be less efficient and is not deemed necessary
         * at this point.
         *
         * Another option would be to expire user's query results when his
         * permissions change, but this can be tricky if the permissions
         * are changed on a group or if the stuff is reimported.
         *
         */
        def res = result.username == user.username

        if (!res) {
            log.warn "Denying $user access to query result $result because " +
                    "its creator (${result.username}) doesn't match the user " +
                    "(${user.username})"
        } else {
            log.debug "Granting $user access to $result (usernames match)"
        }

        res
    }

}
