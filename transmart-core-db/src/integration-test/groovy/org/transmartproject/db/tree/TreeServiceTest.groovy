/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.tree

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.user.User
import spock.lang.Specification

@Rollback
@Integration
class TreeServiceTest extends Specification {

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
                'Private Studies'
        ]
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
                'CATEGORICAL_VALUES',
                'CLINICAL_TRIAL',
                'CLINICAL_TRIAL_HIGHDIM',
                'EHR',
                'EHR_HIGHDIM',
                'MIX_HD',
                'Oracle_1000_Patient',
                'RNASEQ_TRANSCRIPT',
                'SHARED_CONCEPTS_STUDY_A',
                'SHARED_CONCEPTS_STUDY_B',
                'SHARED_HD_CONCEPTS_STUDY_A',
                'SHARED_HD_CONCEPTS_STUDY_B',
                'TUMOR_NORMAL_SAMPLES'

        ]
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
                'Private Studies'
        ]
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
                'Private Studies'
        ]
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
                'Private Studies'
        ]
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

}
