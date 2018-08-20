/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.tree

import grails.transaction.Transactional
import groovy.transform.CompileStatic
import org.apache.commons.lang3.tuple.Pair
import org.grails.core.util.StopWatch
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.ServiceNotAvailableException
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTermTag
import org.transmartproject.core.ontology.OntologyTermTagsResource
import org.transmartproject.core.tree.TreeNode
import org.transmartproject.core.tree.TreeResource
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.SimpleUser
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.clinical.AggregateDataService
import org.transmartproject.db.ontology.I2b2Secure
import org.transmartproject.core.users.User
import org.transmartproject.db.util.SharedLock

import javax.annotation.Resource
import java.util.stream.Collectors

import static grails.async.Promises.task

@Transactional(readOnly = true)
@CompileStatic
class TreeService implements TreeResource {

    @Autowired
    UsersResource usersResource

    @Autowired
    AccessControlChecks accessControlChecks

    @Resource
    OntologyTermTagsResource tagsResource

    @Autowired
    TreeCacheService treeCacheService

    @Autowired
    AggregateDataService aggregateDataService

    @Autowired
    SessionFactory sessionFactory

    /**
     * Adds observation counts and patient counts to leaf nodes and study nodes.
     */
    void enrichWithCounts(List<TreeNode> forest, User user) {
        if (!forest) {
            return
        }
        forest.each { TreeNode it ->
            def node = it as TreeNodeImpl
            if (OntologyTerm.VisualAttributes.LEAF in node.visualAttributes) {
                if (node.tableName == 'concept_dimension' && node.constraint) {
                    def counts = aggregateDataService.counts(node.constraint, user)
                    node.observationCount = counts.observationCount
                    node.patientCount = counts.patientCount
                }
            } else {
                if (OntologyTerm.VisualAttributes.STUDY in node.visualAttributes && node.constraint) {
                    def counts = aggregateDataService.counts(node.constraint, user)
                    node.observationCount = counts.observationCount
                    node.patientCount = counts.patientCount
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

    private I2b2Secure fetchRootNode(User user, String rootKey) {
        if (rootKey == I2b2Secure.ROOT) {
            return null
        }
        I2b2Secure root = (I2b2Secure) I2b2Secure.createCriteria().get {
            eq('fullName', rootKey)
        }
        if (!root || !accessControlChecks.hasAccess(user, root)) {
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
    List<TreeNode> findNodesForUser(String rootKey, Integer depth, Boolean includeCounts, Boolean includeTags, User currentUser) {
        rootKey = rootKey ?: I2b2Secure.ROOT
        depth = depth ?: 0
        includeCounts = includeCounts ?: Boolean.FALSE
        includeTags = includeTags ?: Boolean.FALSE

        I2b2Secure root = fetchRootNode(currentUser, rootKey)
        int maxLevel = depth
        if (depth > 0 && root) {
            maxLevel += root.level
        }
        List<TreeNode> forest = treeCacheService.fetchCachedSubtree(currentUser, rootKey, maxLevel)

        if (includeCounts) {
            def t3 = new Date()
            enrichWithCounts(forest, currentUser)
            def t4 = new Date()
            log.debug "Adding counts took ${t4.time - t3.time} ms."
        }
        if (includeTags) {
            def t5 = new Date()
            enrichWithTags(forest, currentUser)
            def t6 = new Date()
            log.debug "Adding metadata tags took ${t6.time - t5.time} ms."
        }
        forest
    }

    static final private SharedLock lock = new SharedLock()

    /**
     * Checks if a cache rebuild task is active.
     * Only available for administrators.
     *
     * @param currentUser the current user.
     * @return true iff a cache rebuild task is active.
     */
    boolean isRebuildActive(User currentUser) {
        if (!currentUser.admin) {
            throw new AccessDeniedException('Only allowed for administrators.')
        }
        if (lock.tryLock()) {
            lock.unlock()
            return false
        }
        true
    }

    void rebuildCacheTask() {
        def session = null
        try {
            session = sessionFactory.openSession()
            def stopWatch = new StopWatch('Rebuild cache')
            List<User> realUsers = usersResource.getUsers()
            def permissionSets = realUsers.stream()
                    .map({User user -> Pair.of(user.admin, user.studyToPatientDataAccessLevel)})
                    .collect(Collectors.toSet())
            List<User> fakeUsers = permissionSets.stream()
                    .map({ Pair<Boolean, Map<String, PatientDataAccessLevel>> permissions ->
                new SimpleUser('system', null, null, permissions.left, permissions.right)
            })
                    .collect(Collectors.toList())
            for (User user: fakeUsers) {
                def description = "${user.username}${user.admin ? ' (admin)' : ''} ${user.studyToPatientDataAccessLevel.toMapString()}"
                log.info "Rebuilding the cache for user ${description} ..."
                stopWatch.start("Rebuild the tree nodes cache for ${description}")
                treeCacheService.updateSubtreeCache(user, I2b2Secure.ROOT, 0)
                stopWatch.stop()
                stopWatch.start("Rebuild the counts cache for ${description}")
                // Update observations, patients counts
                aggregateDataService.rebuildCountsCacheForUser(user)
                stopWatch.stop()
            }
            for (User user: realUsers) {
                stopWatch.start("Create set of all subjects for ${user.username}")
                aggregateDataService.createAllPatientsSetForUser(user)
                stopWatch.stop()
                stopWatch.start("Rebuild the counts cache for bookmarked queries of ${user.username}")
                aggregateDataService.rebuildCountsCacheForBookmarkedUserQueries(user)
                stopWatch.stop()
            }
            log.info "Done rebuilding the cache.\n${stopWatch.prettyPrint()}"
        } catch (Exception e) {
            log.error "Unexpected error while rebuilding cache: ${e.message}", e
            throw e
        } finally {
            log.debug "Closing task (lock: ${lock.locked})"
            session?.close()
            lock.unlock()
            log.debug "Task closed (lock: ${lock.locked})"
        }
    }

    /**
     * Rebuild the tree nodes and counts caches for every user.
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
    void rebuildCache() throws ServiceNotAvailableException {
        if (!lock.tryLock()) {
            throw new ServiceNotAvailableException('Rebuild operation already in progress.')
        }
        log.info "Rebuild cache started"
        task {
            rebuildCacheTask()
        }
    }

}
