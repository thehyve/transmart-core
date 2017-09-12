/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.tree

import groovy.transform.CompileStatic
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Restrictions
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTermTag
import org.transmartproject.core.ontology.OntologyTermTagsResource
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.ConstraintFactory
import org.transmartproject.db.ontology.I2b2Secure
import org.transmartproject.db.user.User
import org.transmartproject.db.util.StringUtils

import javax.annotation.Resource

class TreeService {

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
    @CompileStatic
    List<TreeNode> findNodesForUser(String rootKey, Integer depth, Boolean includeCounts, Boolean includeTags, User user) {
        rootKey = rootKey ?: I2b2Secure.ROOT
        depth = depth ?: 0
        includeCounts = includeCounts ?: Boolean.FALSE
        includeTags = includeTags ?: Boolean.FALSE

        List<TreeNode> forest
        if (rootKey != I2b2Secure.ROOT || depth > 0) {
            forest = treeCacheService.fetchCachedSubtreeForUser(user, rootKey, depth)
        } else {
            forest = treeCacheService.fetchCachedTreeForUser(user)
        }
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

}
