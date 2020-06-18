/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.tree

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.ontology.OntologyTerm.VisualAttributes
import org.transmartproject.core.tree.TreeNode
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.ontology.I2b2Secure
import org.transmartproject.db.user.User
import spock.lang.Specification

import static org.transmartproject.core.ontology.OntologyTermType.*

@Rollback
@Integration
class TreeServiceSpec extends Specification {

    static final String ADMIN_USER = 'admin'
    static final String PUBLIC_ACCESS_USER = 'test-public-user-1'
    static final String PRIVATE_STUDY_USER = 'test-public-user-2'

    @Autowired
    TreeService treeService

    @Autowired
    UsersResource usersResource

    void 'test tree retrieval for admin user'() {
        User user = (User) usersResource.getUserFromUsername(ADMIN_USER)
        def rootKey = '\\'
        def depth = 0
        def counts = false

        when: "retrieving the full tree for the admin user"
        List<TreeNode> forest = treeService.findNodesForUser(rootKey, depth, counts, false, user)

        then: "the top level should contain all root nodes"
        forest*.name == [
                'Vital Signs',
                'Public Studies',
                'Projects',
                'Private Studies',
                'Pedigree',
                'Interests',
                'General',
                'Demographics',
                'Custom ontologies'
        ].sort()
    }

    void 'test subtree retrieval for admin user'() {
        User user = (User) usersResource.getUserFromUsername(ADMIN_USER)
        def rootKey = '\\Public Studies\\'
        def depth = 0
        def counts = false

        when: "retrieving the subtree starting at \\Public Studies\\ for admin user"
        List<TreeNode> forest = treeService.findNodesForUser(rootKey, depth, counts, false, user)

        then: "the top level should contain one node with all public studies as children"
        forest*.name == ['Public Studies']
        forest[0].children*.name == [
                '100 CATS',
                'CATEGORICAL_VALUES',
                'CLINICAL_TRIAL',
                'CLINICAL_TRIAL_HIGHDIM',
                'CSR',
                'EHR',
                'EHR_HIGHDIM',
                'IMAGES',
                'MIX_HD',
                'Oracle_1000_Patient',
                'RNASEQ_TRANSCRIPT',
                'SHARED_CONCEPTS_STUDY_A',
                'SHARED_CONCEPTS_STUDY_B',
                'SHARED_HD_CONCEPTS_STUDY_A',
                'SHARED_HD_CONCEPTS_STUDY_B',
                'TUMOR_NORMAL_SAMPLES',
                'Multi-DWH Test',
                'Ontology overlap test study'
        ].sort()
    }

    void 'test tree retrieval with limited depth for admin user'() {
        User user = (User) usersResource.getUserFromUsername(ADMIN_USER)
        def rootKey = '\\'
        def depth = 1
        def counts = false

        when: "retrieving the tree with depth 1 for admin user"
        List<TreeNode> forest = treeService.findNodesForUser(rootKey, depth, counts, false, user)

        then: "the top level nodes should have no children"
        forest*.name == [
                'Vital Signs',
                'Public Studies',
                'Projects',
                'Private Studies',
                'Pedigree',
                'Interests',
                'General',
                'Demographics',
                'Custom ontologies'
        ].sort()
        forest*.children.unique() == [null]
    }

    void 'test tree retrieval for public access user'() {
        User user = (User) usersResource.getUserFromUsername(PUBLIC_ACCESS_USER)
        def rootKey = '\\'
        def depth = 0
        def counts = false

        when: "retrieving the full tree for the public access user"
        List<TreeNode> forest = treeService.findNodesForUser(rootKey, depth, counts, false, user)

        then: "the top level should contain all root nodes"
        forest*.name == [
                'Vital Signs',
                'Public Studies',
                'Projects',
                'Private Studies',
                'Pedigree',
                'Interests',
                'General',
                'Demographics',
                'Custom ontologies'
        ].sort()
    }

    void 'test access denied for public access user on private study'() {
        User user = (User) usersResource.getUserFromUsername(PUBLIC_ACCESS_USER)
        def rootKey = '\\Private Studies\\'
        def depth = 0
        def counts = false

        when: "retrieving the private studies subtree for the public access user"
        List<TreeNode> forest = treeService.findNodesForUser(rootKey, depth, counts, false, user)

        then: "the top level node should contain no children"
        forest*.name == ['Private Studies']
        forest[0].children == null

        when: "trying to access a private study without access"
        rootKey = '\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\'
        treeService.findNodesForUser(rootKey, depth, counts, false, user)

        then: "an exception is thrown"
        def e = thrown(AccessDeniedException)
        e.message == 'Access denied to path: \\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\'
    }

    void 'test tree retrieval for private access user'() {
        User user = (User) usersResource.getUserFromUsername(PRIVATE_STUDY_USER)
        def rootKey = '\\'
        def depth = 0
        def counts = false

        when: "retrieving the full tree for the private access user"
        List<TreeNode> forest = treeService.findNodesForUser(rootKey, depth, counts, false, user)

        then: "the top level should contain all root nodes"
        forest*.name == [
                'Vital Signs',
                'Public Studies',
                'Projects',
                'Private Studies',
                'Pedigree',
                'Interests',
                'General',
                'Demographics',
                'Custom ontologies'
        ].sort()
    }

    void 'test access granted for private access user on private study'() {
        User user = (User) usersResource.getUserFromUsername(PRIVATE_STUDY_USER)
        def rootKey = '\\Private Studies\\'
        def depth = 0
        def counts = false

        when: "retrieving the full tree for the public access user"
        List<TreeNode> forest = treeService.findNodesForUser(rootKey, depth, counts, false, user)

        then: "the top level should contain all root nodes"
        forest*.name == ['Private Studies']
        forest[0].children*.name as Set == ['SHARED_HD_CONCEPTS_STUDY_C_PR', 'SHARED_CONCEPTS_STUDY_C_PRIV'] as Set

        when: "trying to access a private study as privileged user"
        rootKey = '\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\'
        forest = treeService.findNodesForUser(rootKey, depth, counts, false, user)

        then: "the result should be the node and its subtree"
        forest*.name == ['SHARED_CONCEPTS_STUDY_C_PRIV']
    }

    /**
     * Collects all tree nodes in one list. Assumes all entries in the argument to be
     * acyclic (otherwise this recurses infinitely).
     * @param forest a list of directed acyclic graphs.
     * @return the list of all nodes in the forest.
     */
    private List<TreeNode> flatten(List<TreeNode> forest) {
        forest ? (forest + forest.collectMany { flatten(it.children) }) : [] as List<TreeNode>
    }

    void 'test correctness of test data'() {
        User user = (User) usersResource.getUserFromUsername(ADMIN_USER)

        when: 'retrieving the full tree for the admin user and all i2b2 entries in the database'
        List<TreeNode> forest = treeService.findNodesForUser(null, null, false, false, user)
        List<TreeNode> nodes = flatten(forest)
        List<I2b2Secure> i2b2Nodes = I2b2Secure.all
        List<ConceptDimension> concepts = ConceptDimension.all
        Set<String> i2b2NodesConceptPaths = i2b2Nodes.findAll {
            it.dimensionTableName.toLowerCase() == 'concept_dimension' && it.columnName.toLowerCase() == 'concept_path'}
        .collect { it.dimensionCode } as Set<String>
        Set<String> allConceptsMinusTreeConcepts = (concepts*.conceptPath as Set<String>) - i2b2NodesConceptPaths
        Set<String> treeConceptsMinusAllConcepts = i2b2NodesConceptPaths - (concepts*.conceptPath as Set<String>)
        Set<String> i2b2NodesMinusTreeNodes = (i2b2Nodes*.fullName as Set<String>) - (nodes*.fullName as Set<String>)
        Set<String> treeNodesMinusI2b2Nodes = (nodes*.fullName as Set<String>) - (i2b2Nodes*.fullName as Set<String>)

        then: 'all paths in the database should be present in the tree'
        i2b2NodesMinusTreeNodes == ([] as Set)

        and: 'all paths in the tree should also exists in the database'
        treeNodesMinusI2b2Nodes == ([] as Set)

        and: 'the set of paths in the database is equal to the set of paths in the tree'
        i2b2Nodes.size() == nodes.size()
        (i2b2Nodes*.fullName as Set<String>) == (nodes*.fullName as Set<String>)

        and: 'all concepts should be referred to by a tree node'
        allConceptsMinusTreeConcepts == ([] as Set)

        // and: 'all concept nodes should refer to an existing concept'
        // treeConceptsMinusAllConcepts == ([] as Set)

        and: 'all leaf nodes have a type'
        nodes.each { TreeNode node ->
            if (VisualAttributes.LEAF in node.visualAttributes) {
                assert node.ontologyTermType in [
                        NUMERIC,
                        TEXT,
                        CATEGORICAL,
                        CATEGORICAL_OPTION,
                        DATE,
                        HIGH_DIMENSIONAL
                ]
            } else if (VisualAttributes.MODIFIER_LEAF in node.visualAttributes) {
                assert node.ontologyTermType == MODIFIER
            }
        }

        and: 'all leaf nodes have a non-empty constraint'
        nodes.each { TreeNode node ->
            if ([VisualAttributes.LEAF, VisualAttributes.MODIFIER_LEAF].any { it in node.visualAttributes}) {
                assert node.constraint != null
            }
        }

        and: 'all study nodes have a study id'
        nodes.each { TreeNode node ->
            if (VisualAttributes.STUDY in node.visualAttributes) {
                assert node.studyId && !node.studyId.empty
            }
        }

        and: 'all study nodes have a non-empty constraint'
        nodes.each { TreeNode node ->
            if (VisualAttributes.STUDY in node.visualAttributes) {
                assert node.constraint != null
            }
        }
    }

}
