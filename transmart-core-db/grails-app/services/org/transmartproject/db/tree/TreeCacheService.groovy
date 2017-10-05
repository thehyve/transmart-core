/* (c) Copyright 2017, The Hyve B.V. */

package org.transmartproject.db.tree

import grails.plugin.cache.CacheEvict
import grails.plugin.cache.Cacheable
import groovy.transform.CompileStatic
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Order
import org.hibernate.criterion.Restrictions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import org.transmartproject.core.tree.TreeNode
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.ontology.I2b2Secure
import org.transmartproject.db.util.StringUtils

@Transactional(readOnly = true)
@CompileStatic
class TreeCacheService {

    static final Logger log = LoggerFactory.getLogger(TreeCacheService.class)

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
    private static List<TreeNode> buildForest(List<I2b2Secure> nodes) {
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
            def l1 = new Date()
            List<I2b2Secure> levelNodes = leveledNodes[level]
            List<TreeNode> previousLevel = forest[level + 1] ?: [] as List<TreeNode>
            Map<String, List<TreeNode>> parentPathToChildNodes = previousLevel.groupBy { parentPath(it.fullName) }
            println "Level ${level}: ${leveledNodes.size()} nodes, ${previousLevel.size()} in the previous level (${parentPathToChildNodes.keySet().size()} groups)."
            List<TreeNode> levelForest = levelNodes.collect { I2b2Secure currentNode ->
                List<TreeNode> children = parentPathToChildNodes[currentNode.fullName]?.sort { TreeNode it -> it.name }
                def node = new TreeNodeImpl(
                        currentNode,
                        children
                )
                node.conceptPath = getConceptPath(node.tableName, node.dimensionCode)
                node.dimension = getDimension(node.tableName, currentNode.code)
                node.children?.each { TreeNode it ->
                    def child = it as TreeNodeImpl
                    child.parent = node
                }
                node as TreeNode
            }
            forest[level] = levelForest
            def l2 = new Date()
            println "Level ${level} took ${l2.time - l1.time} ms."
        }
        def t3 = new Date()
        log.debug "Forest completed in ${t3.time - t1.time} ms."
        def topLevel = levels.min()
        forest[topLevel]
    }

    /**
     * Fetches a subtree for a user. Returns the list of to level tree nodes,
     * with the child nodes embedded.
     *
     * @param isAdmin whether the user is admin or not.
     * @param studyTokens Secure access tokens of the studies the user has access to.
     * @param rootPath restricts to fetching only starting from the specified root element.
     *        (default: null, meaning no restriction.)
     * @param maxLevel restricts to fetching up to the specified level.
     *        (default: 0, meaning no restriction.)
     *
     * @return the list of top level tree nodes with child nodes embedded.
     */
    @Cacheable('org.transmartproject.db.tree.TreeCacheService')
    List<TreeNode> fetchCachedSubtree(boolean isAdmin = false, List<String> studyTokens = [], String rootPath = I2b2Secure.ROOT, int maxLevel = 0) {
        log.info "Fetching tree nodes ..."
        DetachedCriteria criteria = DetachedCriteria.forClass(I2b2Secure)
        if (!isAdmin) {
            List<String> tokens = [Study.PUBLIC, 'EXP:PUBLIC'] + studyTokens
            criteria = criteria.add(Restrictions.in('secureObjectToken', tokens))
        }
        if (rootPath != I2b2Secure.ROOT) {
            criteria.add(StringUtils.startsWith('fullName', rootPath))
        }
        if (maxLevel > 0) {
            criteria.add(Restrictions.lt('level', maxLevel))
        }
        criteria.addOrder(Order.desc('level'))
        criteria.addOrder(Order.desc('fullName'))

        def t1 = new Date()
        List<I2b2Secure> i2b2Nodes = criteria.getExecutableCriteria(sessionFactory.currentSession).list()
        def t2 = new Date()
        log.info "Found ${i2b2Nodes.size()} nodes. Query took ${t2.time - t1.time} ms."

        List<TreeNode> forest = buildForest(i2b2Nodes)
        def t3 = new Date()
        log.debug "Forest growing took ${t3.time - t2.time} ms."

        forest
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
