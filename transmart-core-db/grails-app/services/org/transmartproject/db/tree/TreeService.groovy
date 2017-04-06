/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.tree

import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.hibernate.criterion.Criterion
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Order
import org.hibernate.criterion.Restrictions
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTermTag
import org.transmartproject.core.ontology.OntologyTermTagsResource
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.ConstraintFactory
import org.transmartproject.db.ontology.I2b2Secure
import org.transmartproject.db.user.User
import org.transmartproject.db.util.StringUtils

import javax.annotation.Resource

@Slf4j
class TreeService {

    @Autowired
    AccessControlChecks accessControlChecks

    @Autowired
    MultiDimensionalDataResource multiDimensionalDataResource

    @Resource
    OntologyTermTagsResource tagsResource

    SessionFactory sessionFactory

    public static final String ROOT = '\\'

    /**
     * Create a forest from a flat list of tree nodes, i.e., children will be attached
     * to their parents.
     *
     * @param nodes a flat list of tree nodes.
     * @return a forest of nodes.
     */
    static List<TreeNode> buildForest(List<I2b2Secure> nodes) {
        Map<Integer, List<I2b2Secure>> leveledNodes = nodes.groupBy { it.level }
        Map<Integer, List<TreeNode>> forest = [:]
        def levels = leveledNodes.keySet().sort().reverse()
        if (levels.empty) {
            return []
        }
        levels.each { int level ->
            def levelNodes = leveledNodes[level]
            def lowerBranches = forest[level + 1] ?: []
            def levelForest = []
            levelNodes.each { I2b2Secure currentNode ->
                def children = lowerBranches.findAll { TreeNode branch ->
                    branch.fullName.startsWith(currentNode.fullName)
                }.sort { it.name }
                if (children.empty) {
                    children = null
                }
                def node = new TreeNode(
                        currentNode,
                        children
                )
                node.dimension = getDimension(node.tableName, currentNode.code)
                node.children.each { child ->
                    child.parent = node
                }
                levelForest.add(node)
            }
            forest[level] = levelForest
        }
        def topLevel = levels.min()
        forest[topLevel]
    }

    static Criterion startsWith(String propertyName, String value) {
        StringUtils.like(propertyName, value, MatchMode.START)
    }

    static Criterion like(String propertyName, String value) {
        StringUtils.like(propertyName, value, MatchMode.EXACT)
    }

    static Criterion contains(String propertyName, String value) {
        StringUtils.like(propertyName, value, MatchMode.ANYWHERE)
    }

    static String getDimension(String table, String modifierCode) {
        switch (table) {
            case 'concept_dimension':
                return DimensionImpl.CONCEPT.name
            case 'patient_dimension':
                return DimensionImpl.PATIENT.name
            case 'modifier_dimension':
                def dimension = DimensionDescription.createCriteria().list {
                    eq('modifierCode', modifierCode)
                } as List<DimensionDescription>
                if (dimension.size() > 0) {
                    return dimension.first().name
                }
                break
            case 'trial_visit_dimension':
                return DimensionImpl.TRIAL_VISIT.name
            case 'study':
                return DimensionImpl.STUDY.name
        }
        return 'UNKNOWN'
    }

    /**
     * Adds observation counts and patient counts to leaf nodes.
     */
    void enrichWithCounts(List<TreeNode> forest, User user) {
        forest.each { TreeNode node ->
            if (OntologyTerm.VisualAttributes.LEAF in node.visualAttributes) {
                if (node.tableName == 'concept_dimension' && node.constraint) {
                    Constraint constraint = ConstraintFactory.create(node.constraint)
                    node.observationCount = multiDimensionalDataResource.cachedCount(constraint, user)
                    node.patientCount = multiDimensionalDataResource.cachedPatientCount(constraint, user)
                }
            } else {
                if (OntologyTerm.VisualAttributes.STUDY in node.visualAttributes && node.constraint) {
                    Constraint constraint = ConstraintFactory.create(node.constraint)
                    node.patientCount = multiDimensionalDataResource.cachedPatientCount(constraint, user)
                }
                enrichWithCounts(node.children, user)
            }
        }
    }

    /**
     * Adds metadata tags to tree nodes.
     */
    void enrichWithTags(List<TreeNode> forest, User user) {
        def terms = forest*.delegate as Set<I2b2Secure>
        Map<OntologyTerm, List<OntologyTermTag>> map = tagsResource.getTags(terms, false)
        forest.each { TreeNode node ->
            node.tags = map.get(node.delegate)
            enrichWithTags(node.children, user)
        }
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
    List<TreeNode> findNodesForUser(String rootKey, Integer depth, Boolean includeCounts, Boolean includeTags, User user) {
        rootKey = rootKey ?: ROOT
        depth = depth ?: 0
        includeCounts = includeCounts ?: Boolean.FALSE
        includeTags = includeTags ?: Boolean.FALSE

        List<String> tokens = [Study.PUBLIC, 'EXP:PUBLIC']
        if (!user.admin) {
            Collection<Study> studies = accessControlChecks.getDimensionStudiesForUser(user)
            tokens += studies*.secureObjectToken
        }

        int toplevel = -1
        String prefix = ROOT
        if (rootKey != ROOT) {
            DetachedCriteria criteria = DetachedCriteria.forClass(I2b2Secure)
                    .add(like('fullName', rootKey))
            if (!user.admin) {
                criteria = criteria.add(Restrictions.in('secureObjectToken', tokens))
            }

            def root = criteria.getExecutableCriteria(sessionFactory.currentSession).uniqueResult() as I2b2Secure
            if (!root) {
                throw new AccessDeniedException("Access denied to path: ${rootKey}")
            }
            toplevel = root.level
            prefix = root.fullName
        }

        DetachedCriteria criteria = DetachedCriteria.forClass(I2b2Secure)
                .add(startsWith('fullName', prefix))
                .add(Restrictions.ge('level', toplevel))
        if (!user.admin) {
            criteria = criteria.add(Restrictions.in('secureObjectToken', tokens))
        }
        if (depth > 0) {
            criteria.add(Restrictions.lt('level', toplevel + depth))
        }
        criteria.addOrder(Order.desc('level'))
        criteria.addOrder(Order.desc('fullName'))
        def t1 = new Date()
        List<I2b2Secure> nodes = criteria.getExecutableCriteria(sessionFactory.currentSession).list()
        nodes.unique { it.fullName }
        def t2 = new Date()
        log.debug "Found ${nodes.size()} nodes. Query took ${t2.time - t1.time} ms."

        def forest = buildForest(nodes)
        def t3 = new Date()
        log.debug "Forest growing took ${t3.time - t2.time} ms."
        if (includeCounts) {
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
}
