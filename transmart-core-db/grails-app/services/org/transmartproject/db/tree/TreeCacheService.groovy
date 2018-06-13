/* (c) Copyright 2017, The Hyve B.V. */

package org.transmartproject.db.tree

import grails.plugin.cache.CacheEvict
import grails.plugin.cache.CachePut
import grails.plugin.cache.Cacheable
import groovy.transform.CompileStatic
import org.grails.core.util.StopWatch
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Order
import org.hibernate.criterion.Restrictions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import org.transmartproject.core.concept.ConceptsResource
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.tree.TreeNode
import org.transmartproject.core.users.AccessLevel
import org.transmartproject.core.users.User
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.ontology.I2b2Secure
import org.transmartproject.db.util.StringUtils

@Transactional(readOnly = true)
@CompileStatic
class TreeCacheService {

    static final Logger log = LoggerFactory.getLogger(TreeCacheService.class)

    @Autowired
    ConceptsResource conceptsResource

    @Autowired
    AccessControlChecks accessControlChecks

    SessionFactory sessionFactory

    static final String parentPath(String path) {
        path[0 .. path.lastIndexOf('\\', path.size() - 2)]
    }

    static final String getConceptPath(String table, String code){
        return table == 'concept_dimension' ? code : null
    }

    private static String getModifierDimension(String modifierCode) {
        DimensionDescription.getDimensionByModifierCode(modifierCode)?.name
    }

    static final String getDimension(String table, String modifierCode) {
        switch (table) {
            case 'concept_dimension':
                return DimensionImpl.CONCEPT.name
            case 'patient_dimension':
                return DimensionImpl.PATIENT.name
            case 'modifier_dimension':
                return getModifierDimension(modifierCode)
            case 'trial_visit_dimension':
                return DimensionImpl.TRIAL_VISIT.name
            case 'study':
                return DimensionImpl.STUDY.name
        }
        return 'UNKNOWN'
    }

    /**
     * Create a forest from a flat list of tree nodes, i.e., children will be attached
     * to their parents.
     *
     * @param nodes a flat list of tree nodes.
     * @return a forest of nodes.
     */
    private List<TreeNode> buildForest(List<I2b2Secure> nodes) {
        log.debug "Building forest ..."
        def t1 = new Date()
        Map<Integer, List<I2b2Secure>> leveledNodes = nodes.groupBy { it.level }
        Map<Integer, List<TreeNode>> forest = [:]
        def levels = leveledNodes.keySet().sort().reverse()
        def t2 = new Date()
        log.debug "Init took ${t2.time - t1.time} ms."
        if (levels.empty) {
            return []
        }
        levels.each { int level ->
            List<I2b2Secure> levelNodes = leveledNodes[level]
            List<TreeNode> previousLevel = forest[level + 1] ?: [] as List<TreeNode>
            Map<String, List<TreeNode>> parentPathToChildNodes = previousLevel.groupBy { parentPath(it.fullName) }
            List<TreeNode> levelForest = levelNodes.collect { I2b2Secure currentNode ->
                List<TreeNode> children = parentPathToChildNodes[currentNode.fullName]?.sort { TreeNode it -> it.name }
                def node = new TreeNodeImpl(
                        currentNode,
                        children
                )
                node.children?.each { TreeNode it ->
                    def child = it as TreeNodeImpl
                    child.parent = node
                }
                if (OntologyTerm.VisualAttributes.LEAF in node.visualAttributes) {
                    node.conceptPath = getConceptPath(node.tableName, node.dimensionCode)
                    if (node.conceptPath) {
                        node.conceptCode = conceptsResource.getConceptCodeByConceptPath(node.conceptPath)
                    }
                    node.dimension = getDimension(node.tableName, currentNode.code)
                }
                node as TreeNode
            }
            forest[level] = levelForest
        }
        def t3 = new Date()
        log.debug "Forest completed in ${t3.time - t1.time} ms."
        def topLevel = levels.min()
        forest[topLevel]
    }

    List<TreeNode> fetchSubtree(User user, String rootPath = I2b2Secure.ROOT, int maxLevel = 0) {
        log.info "Fetching tree nodes ..."
        DetachedCriteria criteria = DetachedCriteria.forClass(I2b2Secure)
        if (rootPath != I2b2Secure.ROOT) {
            criteria.add(StringUtils.startsWith('fullName', rootPath))
        }
        if (maxLevel > 0) {
            criteria.add(Restrictions.lt('level', maxLevel))
        }
        criteria.addOrder(Order.desc('level'))
        criteria.addOrder(Order.desc('fullName'))

        def stopWatch = new StopWatch('Collecting tree nodes.')
        stopWatch.start('Fetching nodes from the database.')
        List<I2b2Secure> i2b2Nodes = criteria.getExecutableCriteria(sessionFactory.currentSession).list()
        stopWatch.stop()
        stopWatch.start('Filtering nodes user has access to.')
        List<I2b2Secure> accessibleI2b2Nodes = i2b2Nodes.findAll { I2b2Secure i2b2Secure ->
            accessControlChecks.canPerform(user, AccessLevel.AGGREGATE_WITH_THRESHOLD, i2b2Secure) }
        stopWatch.stop()
        log.debug("${accessibleI2b2Nodes.size()} from ${i2b2Nodes.size()} are accessible to ${user} user.")
        stopWatch.start('Building the forest for accessible nodes.')
        List<TreeNode> forest = buildForest(accessibleI2b2Nodes)
        stopWatch.stop()

        log.debug('Fetching subtree report:\n' + stopWatch.prettyPrint())

        forest
    }

    @CachePut(value = 'org.transmartproject.db.tree.TreeCacheService',
            key = '{ #user.username, #rootPath, #maxLevel }')
    List<TreeNode> updateSubtreeCache(User user, String rootPath = I2b2Secure.ROOT, int maxLevel = 0) {
        fetchSubtree(user, rootPath, maxLevel)
    }

    /**
     * Fetches a subtree for a user. Returns the list of to level tree nodes,
     * with the child nodes embedded.
     *
     * @param user current user
     * @param rootPath restricts to fetching only starting from the specified root element.
     *        (default: null, meaning no restriction.)
     * @param maxLevel restricts to fetching up to the specified level.
     *        (default: 0, meaning no restriction.)
     *
     * @return the list of top level tree nodes with child nodes embedded.
     */
    @Cacheable(value = 'org.transmartproject.db.tree.TreeCacheService',
            key = '{ #user.username, #rootPath, #maxLevel }')
    List<TreeNode> fetchCachedSubtree(User user, String rootPath = I2b2Secure.ROOT, int maxLevel = 0) {
        fetchSubtree(user, rootPath, maxLevel)
    }

    /**
     * Clears the tree node cache. This function should be called after loading, removing or updating
     * tree nodes in the database.
     */
    @CacheEvict(value = 'org.transmartproject.db.tree.TreeCacheService', allEntries = true)
    void clearAllCacheEntries() {
        log.info 'Clearing tree nodes cache ...'
    }

}
