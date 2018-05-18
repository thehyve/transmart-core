/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.accesscontrol

import grails.gorm.DetachedCriteria
import grails.transaction.Transactional
import groovy.util.logging.Slf4j
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Subqueries
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTermsResource
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.ontology.Study
import org.transmartproject.core.querytool.Item
import org.transmartproject.core.querytool.Panel
import org.transmartproject.core.querytool.QueryDefinition
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.core.concept.ConceptKey
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.ontology.AbstractI2b2Metadata
import org.transmartproject.db.ontology.I2b2Secure
import org.transmartproject.db.user.User
import org.transmartproject.db.util.StringUtils

import static org.transmartproject.db.ontology.AbstractAcrossTrialsOntologyTerm.ACROSS_TRIALS_TABLE_CODE

/**
 * Access control methods for use in {@link org.transmartproject.db.user.User}.
 *
 * Right now these are implemented in this same class.
 * If this list gets bigger or needs to be pluggable by other plugins, then
 * this should be refactored into a different solution (e.g. each check
 * implemented in a different Spring bean).
 */
@Slf4j
@Component
class AccessControlChecks {

    public static final String PUBLIC_SOT = 'EXP:PUBLIC'
    public static final Set<String> PUBLIC_TOKENS = [org.transmartproject.db.i2b2data.Study.PUBLIC, PUBLIC_SOT]

    @Autowired
    OntologyTermsResource conceptsResource

    @Autowired
    StudiesResource studiesResource

    @Autowired
    SessionFactory sessionFactory

    Session getSession() {
        sessionFactory.currentSession
    }

    boolean canPerform(User user,
                       ProtectedOperation protectedOperation,
                       I2b2Secure secure) {

        if (user.admin) {
            log.debug "Bypassing check for $protectedOperation on " +
                    "${secure.fullName} for user ${user.username} because she is an " +
                    "administrator"
            return true
        }

        String token = secure.secureObjectToken
        if (!token) {
            throw new UnexpectedResultException("Found i2b2secure object with empty token")
        }
        log.debug "Token for ${secure.fullName} is $token"

        if (token in PUBLIC_TOKENS) {
            return true
        }

        protectedOperation in user.accessStudyTokenToOperations.get(token)
    }

    /**
     * Checks if a {@link org.transmartproject.db.i2b2data.Study} (in the i2b2demodata schema)
     * exists to which the user has access.
     * Access is checked based on the {@link org.transmartproject.db.i2b2data.Study#secureObjectToken}
     * field.
     * This is the <code>/v2</code> way of checking study based access.
     *
     * @param user the user to check access for.
     * @param protectedOperation is ignored.
     * @param study the study object that is referred to from the trial visit dimension.
     * @return true iff a study exists that the user has access to.
     */
    boolean canPerform(User user,
                       ProtectedOperation protectedOperation,
                       org.transmartproject.db.i2b2data.Study study) {
        if (user.admin) {
            log.debug "Bypassing check for $protectedOperation on ${study.studyId} for user ${user.username}" +
                    ' because she is an administrator'
            return true
        }

        String token = study.secureObjectToken
        if (!token) {
            throw new UnexpectedResultException("${study.studyId} study has empty token.")
        }
        log.debug "Token for ${study.studyId} is $token"

        if (token in PUBLIC_TOKENS) {
            return true
        }

        protectedOperation in user.accessStudyTokenToOperations.get(study.secureObjectToken)
    }

    /**
     * Checks if a {@link I2b2Secure} node, representing the study, exists to which the user has access.
     * Access is granted if such a node does not exist.
     * If the node exists, access is checked based on the {@link I2b2Secure#secureObjectToken}
     * field.
     * Warning: this is the <code>/v1</code> way of checking study based access, do not use for
     * <code>/v2</code> code!
     *
     * @param user the user to check access for.
     * @param protectedOperation is ignored.
     * @param study the core API study object representing the study.
     * @return true iff
     * - a study node does not exist in I2b2Secure
     * - or a study node exists in I2b2Secure that the user has access to.
     */
    @Deprecated
    boolean canPerform(User user,
                       ProtectedOperation protectedOperation,
                       Study study) {
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

        canPerform(user, protectedOperation, secure)
    }

    /* Study is included if the user has ANY kind of access */
    Set<Study> getAccessibleStudiesForUser(User user) {
        /* this method could benefit from caching */
        def studySet = studiesResource.studySet
        def mostLimitedOperation = ProtectedOperation.WellKnownOperations.SHOW_SUMMARY_STATISTICS
        studySet.findAll {
            OntologyTerm ontologyTerm = it.ontologyTerm
            assert ontologyTerm : "No ontology node found for study ${it.id}."
            I2b2Secure i2b2Secure = ontologyTerm instanceof I2b2Secure ? ontologyTerm : I2b2Secure.findByFullName(ontologyTerm.fullName)
            if (!i2b2Secure) {
                log.debug("No secure record for ${ontologyTerm.fullName} path found. Study treated as public.")
                return true
            }

            canPerform(user, mostLimitedOperation, i2b2Secure)
        }
    }

    boolean hasUnlimitedStudiesAccess(User user) {
        user.admin
    }

    /* Study is included if the user has ANY kind of access */
    @Transactional(readOnly = true)
    Collection<org.transmartproject.db.i2b2data.Study> getDimensionStudiesForUser(User user) {
        if (hasUnlimitedStudiesAccess(user)) {
            return org.transmartproject.db.i2b2data.Study.findAll()
        }

        def accessibleStudyTokens = getAccessibleStudyTokensForUser(user)
        org.transmartproject.db.i2b2data.Study.findAllBySecureObjectTokenInList(accessibleStudyTokens)
    }

    /**
     * Gets all tokens to the studies user have access to including PUBLIC tokens.
     * @param user
     */
    private static Set<String> getAccessibleStudyTokensForUser(User user) {
        user.accessStudyTokenToOperations.keySet() + PUBLIC_TOKENS
    }

    private boolean exists(org.hibernate.criterion.DetachedCriteria criteria) {
        (criteria.getExecutableCriteria(sessionFactory.currentSession).setMaxResults(1).uniqueResult() != null)
    }

    Set<Dimension> getInaccessibleDimensions(Collection<Dimension> dimensions, User user) {
        def studies = getDimensionStudiesForUser(user)
        def validDimensions = studies*.dimensions?.flatten() as Set
        def result = new LinkedHashSet(dimensions)
        result.removeAll validDimensions
        result
    }

    /**
     * Checks if a concept exists with the provided concept path or concept code
     * that is being referred to from a node {@link I2b2Secure} table that the user
     * has access to.
     * Only one of conceptPath or conceptCode should be provided.
     *
     * Example:
     * <code>checkConceptAccess(conceptPath: '\foo\bar\', user)</code>
     *
     * TODO:
     * This query does not cover many cases, e.g., where {@link I2b2Secure} points
     * to concepts using concept_cd, uses other operators than 'like' or uses prefixes
     * instead of exact matches.
     *
     * @param args the map that should contain either a conceptCode or a conceptPath entry.
     * @param user the user to check access for.
     * @return true iff an entry in {@link I2b2Secure} exists that the user has access to
     * and that refers to a concept in the concept dimension with the provided conceptPath
     * or conceptCode.
     * @throws AccessDeniedException iff none or both of conceptCode and conceptPath are provided.
     */
    public boolean checkConceptAccess(Map args, User user) {
        def conceptPath = args.conceptPath as String
        def conceptCode = conceptPath ? null : args.conceptCode as String
        if (conceptPath == null || conceptPath.empty) {
            if (conceptCode == null || conceptCode.empty) {
                throw new AccessDeniedException("Either concept path or concept code is required.")
            }
        } else if (conceptCode != null && !conceptCode.empty) {
            throw new AccessDeniedException("Got both concept path and concept code. Only one is allowed.")
        }

        if (user.admin) {
            return true
        }

        Set<String> tokens = getAccessibleStudyTokensForUser(user)

        org.hibernate.criterion.DetachedCriteria conceptCriteria =
                org.hibernate.criterion.DetachedCriteria.forClass(ConceptDimension)
        if (conceptPath) {
            conceptCriteria = conceptCriteria.add(StringUtils.like('conceptPath', conceptPath, MatchMode.EXACT))
        } else {
            conceptCriteria = conceptCriteria.add(StringUtils.like('conceptCode', conceptCode, MatchMode.EXACT))
        }
        conceptCriteria = conceptCriteria.setProjection(Projections.property('conceptPath'))

        org.hibernate.criterion.DetachedCriteria criteria = org.hibernate.criterion.DetachedCriteria.forClass(I2b2Secure)
                .add(Restrictions.in('secureObjectToken', tokens))
                .add(Restrictions.ilike('dimensionTableName', 'concept_dimension'))
                .add(Restrictions.ilike('columnName', 'concept_path'))
                .add(Restrictions.ilike('operator', 'like'))
                .add(Subqueries.propertyIn('dimensionCode', conceptCriteria))
        exists(criteria)
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
            Set<Study> foundStudies = panel.items.findAll { Item item ->
                /* always permit across trial nodes.
                 * Across trial terms have no study, so they have to be
                 * handled especially */
                new ConceptKey(item.conceptKey).tableCode !=
                       ACROSS_TRIALS_TABLE_CODE
            }.collect { Item item ->
                /* this could be optimized by adding a new method in
                 * StudiesResource */
                def concept = conceptsResource.getByKey(item.conceptKey)
                if (concept instanceof AbstractI2b2Metadata) {
                    if (concept.dimensionCode != concept.fullName) {
                        log.warn "Shared concepts not supported. Term: ${concept.fullName}, concept: ${concept.dimensionCode}"
                        throw new AccessDeniedException("Shared concepts cannot be used for cohort selection.")
                    }
                } else {
                    throw new AccessDeniedException("Node type not supported: ${concept?.class?.simpleName}.")
                }

                def study = concept.study

                if (study == null) {
                    log.info "User included concept with no study: ${concept.fullName}"
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
            log.warn "User ${user.username} defined access for definition ${definition.name} " +
                    "because it doesn't include one non-inverted panel for" +
                    "which the user has permission in all the terms' studies"
        } else {
            log.debug "Granting access to user ${user.username} to use " +
                    "query definition ${definition.name}"
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
            log.debug "Granting ${user.username} access to $result (usernames match)"
        }

        res
    }

}
