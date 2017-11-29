/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.tree

import grails.transaction.Transactional
import groovy.transform.CompileStatic
import org.grails.core.util.StopWatch
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Restrictions
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.ServiceNotAvailableException
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTermTag
import org.transmartproject.core.ontology.OntologyTermTagsResource
import org.transmartproject.core.tree.TreeNode
import org.transmartproject.core.tree.TreeResource
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.multidimquery.query.TrueConstraint
import org.transmartproject.db.ontology.I2b2Secure
import org.transmartproject.db.user.User as DbUser
import org.transmartproject.core.users.User
import org.transmartproject.db.util.StringUtils

import javax.annotation.Resource
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import static grails.async.Promises.task

@Transactional(readOnly = true)
@CompileStatic
class TreeService implements TreeResource {

    @Autowired
    UsersResource usersResource

    @Autowired
    AccessControlChecks accessControlChecks

    @Autowired
    MultiDimensionalDataResource multiDimensionalDataResource

    @Resource
    OntologyTermTagsResource tagsResource

    @Autowired
    TreeCacheService treeCacheService

    SessionFactory sessionFactory

    /**
     * Adds observation counts and patient counts to leaf nodes.
     */
    void enrichWithCounts(List<TreeNode> forest, User user) {
        if (!forest) {
            return
        }
        forest.each { TreeNode it ->
            def node = it as TreeNodeImpl
            if (OntologyTerm.VisualAttributes.LEAF in node.visualAttributes) {
                if (node.tableName == 'concept_dimension' && node.constraint) {
                    def counts = multiDimensionalDataResource.cachedCounts(node.constraint, user)
                    node.observationCount = counts.observationCount
                    node.patientCount = counts.patientCount
                }
            } else {
                if (OntologyTerm.VisualAttributes.STUDY in node.visualAttributes && node.constraint) {
                    node.patientCount = multiDimensionalDataResource.cachedPatientCount(node.constraint, user)
                }
                enrichWithCounts(node.children, user)
            }
        }
    }

    /**
     * Adds metadata tags to tree nodes.
     */
    void enrichWithTags(List<TreeNode> forest, User user) {
        if (!forest) {
            return
        }
        def termPaths = forest*.fullName as Set<String>
        Map<String, List<OntologyTermTag>> map = tagsResource.getTags(termPaths)
        forest.each { TreeNode it ->
            def node = it as TreeNodeImpl
            node.tags = map.get(node.fullName)
            enrichWithTags(node.children, user)
        }
    }

    private List<String> getStudyTokens(DbUser user) {
        List<String> studyTokens = []
        if (!user.admin) {
            Collection<Study> studies = accessControlChecks.getDimensionStudiesForUser(user) as Collection<Study>
            studyTokens = studies*.secureObjectToken
        }
        studyTokens
    }

    private I2b2Secure fetchRootNode(DbUser user, String rootKey) {
        if (rootKey == I2b2Secure.ROOT) {
            return null
        }
        DetachedCriteria criteria = DetachedCriteria.forClass(I2b2Secure)
                .add(StringUtils.like('fullName', rootKey))

        if (!user.admin) {
            List<String> tokens = [Study.PUBLIC, 'EXP:PUBLIC'] + getStudyTokens(user)
            criteria = criteria.add(Restrictions.in('secureObjectToken', tokens))
        }

        I2b2Secure root = criteria.getExecutableCriteria(sessionFactory.currentSession).uniqueResult() as I2b2Secure
        if (!root) {
            throw new AccessDeniedException("Access denied to path: ${rootKey}")
        }
        root
    }

    /**
     * Finds tree nodes with prefix $rootKey up to depth $depth lower than the $rootKey level.
     * If $counts is true, observation counts and patient counts will be added to leaf nodes.
     *
     * @param rootKey the key of the root element, used as prefix. Default value is '\'.
     * If another key is provided, it must specify an existing node to which the user has access.
     * The root node will be included in the results, '\' is not (unless it is an existing node).
     * @param depth the maximum number of levels below the root element that should be returned
     * (default: 0, meaning no limit).
     * @param includeCounts flag if counts should be added to tree nodes (default: false).
     * @param includeTags flag if tag metadata should be added to tree nodes (default: false).
     * @param user the current user.
     * @return a forest, represented as a list of the top level nodes. Lower nodes will be children
     * of their ancestor nodes.
     */
    List<TreeNode> findNodesForUser(String rootKey, Integer depth, Boolean includeCounts, Boolean includeTags, org.transmartproject.core.users.User currentUser) {
        DbUser user = (DbUser) usersResource.getUserFromUsername(currentUser.username)
        rootKey = rootKey ?: I2b2Secure.ROOT
        depth = depth ?: 0
        includeCounts = includeCounts ?: Boolean.FALSE
        includeTags = includeTags ?: Boolean.FALSE

        I2b2Secure root = fetchRootNode(user, rootKey)
        int maxLevel = depth
        if (depth > 0 && root) {
            maxLevel += root.level
        }
        List<TreeNode> forest = treeCacheService.fetchCachedSubtree(user.admin, getStudyTokens(user), rootKey, maxLevel)

        if (includeCounts) {
            def t3 = new Date()
            enrichWithCounts(forest, user)
            def t4 = new Date()
            log.debug "Adding counts took ${t4.time - t3.time} ms."
        }
        if (includeTags) {
            def t5 = new Date()
            enrichWithTags(forest, user)
            def t6 = new Date()
            log.debug "Adding metadata tags took ${t6.time - t5.time} ms."
        }
        forest
    }

    /**
     * Clears the tree node cache and the counts caches.
     * This function should be called after loading, removing or updating
     * tree nodes or observations in the database.
     * Only available for administrators.
     *
     * @param currentUser the current user.
     *
     */
    void clearCache(User currentUser) {
        DbUser user = (DbUser) usersResource.getUserFromUsername(currentUser.username)
        if (!user.admin) {
            throw new AccessDeniedException('Only allowed for administrators.')
        }
        treeCacheService.clearAllCacheEntries()
        multiDimensionalDataResource.clearCountsCache()
        multiDimensionalDataResource.clearPatientCountCache()
        multiDimensionalDataResource.clearCountsPerConceptCache()
    }

    static class SimpleLock {
        boolean locked
    }

    static final private SimpleLock sharedLock = new SimpleLock(locked: false)
    static final private Lock lockLock = new ReentrantLock()

    boolean tryLock() {
        lockLock.lock()
        if (sharedLock.locked) {
            lockLock.unlock()
            return false
        } else {
            sharedLock.locked = true
            lockLock.unlock()
            return true
        }
    }

    void unlock() {
        lockLock.lock()
        sharedLock.locked = false
        lockLock.unlock()
    }

    /**
     * Checks if a cache rebuild task is active.
     * Only available for administrators.
     *
     * @param currentUser the current user.
     * @return true iff a cache rebuild task is active.
     */
    boolean isRebuildActive(User currentUser) {
        DbUser dbUser = (DbUser) usersResource.getUserFromUsername(currentUser.username)
        if (!dbUser.admin) {
            throw new AccessDeniedException('Only allowed for administrators.')
        }
        if (tryLock()) {
            unlock()
            return false
        }
        true
    }

    /**
     * Clears the tree node cache and the counts caches, and
     * rebuild the tree node cache for every user.
     *
     * This function should be called after loading, removing or updating
     * tree nodes or observations in the database.
     * Only available for administrators.
     *
     * Asynchronous call. The call returns when rebuilding has started.
     *
     * @param currentUser the current user.
     * @throws ServiceNotAvailableException iff a rebuild operation is already in progress.
     */
    void rebuildCache(User currentUser) throws ServiceNotAvailableException {
        DbUser dbUser = (DbUser) usersResource.getUserFromUsername(currentUser.username)
        if (!dbUser.admin) {
            throw new AccessDeniedException('Only allowed for administrators.')
        }
        if (!tryLock()) {
            throw new ServiceNotAvailableException('Rebuild operation already in progress.')
        }
        log.debug "Starting task (lock: ${sharedLock.locked})"
        task {
            log.debug "Task started (lock: ${sharedLock.locked})"
            def session = sessionFactory.openSession()
            try {
                def stopWatch = new StopWatch('Rebuild cache')
                log.info 'Clearing all caches ...'
                stopWatch.start('Clearing the caches')
                clearCache(currentUser)
                stopWatch.stop()
                usersResource.getUsers().each {
                    DbUser user = (DbUser)it
                    log.info "Rebuilding the cache for user ${user.username} ..."
                    stopWatch.start("Rebuild the cache for ${user.username}")
                    treeCacheService.fetchCachedSubtree(user.admin, getStudyTokens(user), I2b2Secure.ROOT, 0)
                    //triggers caching counts per concepts for the given user
                    multiDimensionalDataResource.countsPerStudyAndConcept(new TrueConstraint(), user)
                    stopWatch.stop()
                }
                log.info "Done rebuilding the cache.\n${stopWatch.prettyPrint()}"
            } catch (Exception e) {
                log.error "Unexpected error while rebuilding cache: ${e.message}", e
                throw e
            } finally {
                log.debug "Closing task (lock: ${sharedLock.locked})"
                session?.close()
                unlock()
                log.debug "Task closed (lock: ${sharedLock.locked})"
            }
        }
    }

}
