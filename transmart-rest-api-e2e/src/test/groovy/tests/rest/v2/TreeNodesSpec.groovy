/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static config.Config.*

/**
 *  TMPREQ-6 Building a tree where concepts are study-specific
 */
@RequiresStudy([SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID, SHARED_CONCEPTS_RESTRICTED_ID])
class TreeNodesSpec extends RESTSpec {

    /**
     *  given: "Study SHARED_CONCEPTS_STUDY_C_PRIV and SHARED_CONCEPTS_A is loaded, and I have access"
     *  when: "I try to get the tree_nodes from all studies"
     *  then: "nodes from SHARED_CONCEPTS_STUDY_C_PRIV and SHARED_CONCEPTS_A are included"
     */
    def "restricted tree_nodes are included"() {
        given: "Study SHARED_CONCEPTS_STUDY_C_PRIV and SHARED_CONCEPTS_A is loaded, and I do not have access"

        when: "I try to get the tree_nodes from all studies"
        def responseData = get([
                path      : PATH_TREE_NODES,
                acceptType: JSON,
                user      : UNRESTRICTED_USER
        ])

        then: "only nodes from SHARED_CONCEPTS_A are returned"
        def studyA = getNodeByName(getRootNodeByName(responseData, 'Public Studies'), SHARED_CONCEPTS_A_ID)
        def studyC = getNodeByName(getRootNodeByName(responseData, 'Private Studies'), SHARED_CONCEPTS_RESTRICTED_ID)

        assert getChildrenAtributes(studyA, 'name').containsAll([SHARED_CONCEPTS_A_ID, 'Vital Signs', 'Heart Rate', 'Demography', 'Age'])
        assert getChildrenAtributes(studyC, 'name').containsAll([SHARED_CONCEPTS_RESTRICTED_ID, 'Vital Signs', 'Heart Rate', 'Demography', 'Age'])
    }

    /**
     *  given: "Study SHARED_CONCEPTS_STUDY_C_PRIV and SHARED_CONCEPTS_A is loaded, and I do not have access"
     *  when: "I try to get the tree_nodes from all studies"
     *  then: "only nodes from SHARED_CONCEPTS_A are returned"
     */
    def "restricted tree_nodes are excluded"() {
        given: "Study SHARED_CONCEPTS_STUDY_C_PRIV and SHARED_CONCEPTS_A is loaded, and I do not have access"

        when: "I try to get the tree_nodes from all studies"
        def responseData = get([
                path      : PATH_TREE_NODES,
                acceptType: JSON,
        ])

        then: "only nodes from SHARED_CONCEPTS_A are returned"
        def studyA = getNodeByName(getRootNodeByName(responseData, 'Public Studies'), SHARED_CONCEPTS_A_ID)
        def studyC = getNodeByName(getRootNodeByName(responseData, 'Private Studies'), SHARED_CONCEPTS_RESTRICTED_ID)

        assert getChildrenAtributes(studyA, 'name').containsAll([SHARED_CONCEPTS_A_ID, 'Vital Signs', 'Heart Rate', 'Demography', 'Age'])
        assert studyC == null
    }

    /**
     *  given: "Study SHARED_CONCEPTS is loaded"
     *  when: "I get the tree_nodes with a subNode and depth"
     *  then: "only de subNodes of that node are returned"
     */
    def "params limit nodes returned"() {
        given: "Study SHARED_CONCEPTS is loaded"

        when: "I get the tree_nodes with a subNode and depth"
        def responseData = get([
                path      : PATH_TREE_NODES,
                acceptType: JSON,
                query     : [root: "\\Public Studies\\SHARED_CONCEPTS_STUDY_A\\", depth: 2]
        ])

        then: "only de subNodes of that node are returned"
        assert responseData.size() == 1
        def studyA = getRootNodeByName(responseData, SHARED_CONCEPTS_A_ID)

        assert getChildrenAtributes(studyA, 'name').containsAll([SHARED_CONCEPTS_A_ID, 'Vital Signs', 'Demography'])
    }

    /**
     *  given: "Study SHARED_CONCEPTS is loaded"
     *  when: "I get the tree_nodes with counts=true"
     *  then: "then concept nodes have observationCount and patientCount"
     */
    def "nodes with counts true"() {
        given: "Study SHARED_CONCEPTS is loaded"

        when: "I get the tree_nodes with counts=true"
        def responseData = get([
                path      : PATH_TREE_NODES,
                acceptType: JSON,
                query     : ['counts': true]
        ])

        then: "then concept nodes have observationCount and patientCount"
        def studyA = getNodeByName(getRootNodeByName(responseData, 'Public Studies'), SHARED_CONCEPTS_A_ID)

        assert getNodeByName(studyA, "Heart Rate").observationCount == 3
        assert getNodeByName(studyA, "Heart Rate").patientCount == 2
        assert getNodeByName(studyA, "Age").observationCount == 2
        assert getNodeByName(studyA, "Age").patientCount == 2
    }

    /**
     *  given: "Study SHARED_CONCEPTS is loaded"
     *  when: "I get the tree_nodes with counts=true"
     *  then: "then concept nodes have observationCount and patientCount"
     */
    def "shared nodes with counts true restricted"() {
        given: "Study SHARED_CONCEPTS is loaded"

        when: "I get the tree_nodes with counts=true"
        def responseData = get([
                path      : PATH_TREE_NODES,
                acceptType: JSON,
                query     : ['counts': true]
        ])

        then: "then concept nodes have observationCount and patientCount"
        def sharedConceptRoot = getRootNodeByName(responseData, 'Vital Signs')

        assert getNodeByName(sharedConceptRoot, "Heart Rate").observationCount == 5
        assert getNodeByName(sharedConceptRoot, "Heart Rate").patientCount == 4
    }

    /**
     *  given: "Study SHARED_CONCEPTS is loaded"
     *  when: "I get the tree_nodes with counts=true"
     *  then: "then concept nodes have observationCount and patientCount"
     */
    def "nodes with counts true unrestricted"() {
        given: "Study SHARED_CONCEPTS is loaded"

        when: "I get the tree_nodes with counts=true"
        def responseData = get([
                path      : PATH_TREE_NODES,
                acceptType: JSON,
                query     : ['counts': true],
                user      : UNRESTRICTED_USER
        ])

        then: "then concept nodes have observationCount and patientCount"
        def sharedConceptRoot = getRootNodeByName(responseData, 'Vital Signs')

        assert getNodeByName(sharedConceptRoot, "Heart Rate").observationCount == 7
        assert getNodeByName(sharedConceptRoot, "Heart Rate").patientCount == 6
    }

    /**
     *  given: "Study SURVEY1 is loaded"
     *  when: "I get the tree_nodes with tags=true"
     *  then: "then the node for concept Gender has metadata tags"
     */
    def "nodes with tags true"() {
        given: 'Study SURVEY1 is loaded'

        when: 'I get the tree_nodes with tags=true'
        def responseData = get([
                path      : PATH_TREE_NODES,
                acceptType: JSON,
                query     : ['tags': true]
        ])

        then: 'then the node \\Projects\\Survey 1\\Demographics\\Gender has metadata tags'
        def projectsRoot = getRootNodeByName(responseData, 'Projects')
        def projectNode = getNodeByName(projectsRoot, 'Survey 1')
        def demographicsNode = getNodeByName(projectNode, 'Demographics')
        def conceptNode = getNodeByName(demographicsNode, 'Gender')
        assert conceptNode.metadata != null
        assert conceptNode.metadata.containsKey('item_name')
        assert conceptNode.metadata.item_name == 'sex'
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
     *  when: "I try to get the tree_nodes from that study"
     *  then: "I get an access error"
     */
    def "restricted tree_nodes"() {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
        def path = "\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\"

        when: "I try to get the tree_nodes from that study"
        def responseData = get([
                path      : PATH_TREE_NODES,
                acceptType: JSON,
                query     : [root: path, depth: 1],
                statusCode: 403
        ])

        then: "I get an access error"
        assert responseData.httpStatus == 403
        assert responseData.type == 'AccessDeniedException'
        assert responseData.message == "Access denied to path: ${path}"
    }

    def "clear tree node cache as admin"() {
        when: "I try to clear the tree node cache as admin"
        def responseData = get([
                path      : PATH_TREE_NODES_CLEAR_CACHE,
                acceptType: JSON,
                user      : ADMIN_USER,
        ])
        then: "an empty body is returned"
        assert responseData == null
    }

    def "clear tree node cache"() {
        when: "I try to clear the tree node cache as a regular user"
        def responseData = get([
                path      : PATH_TREE_NODES_CLEAR_CACHE,
                acceptType: JSON,
                statusCode: 403
        ])
        then: "access is denied"
        assert responseData.httpStatus == 403
    }

    def getRootNodeByName(list, name) {
        def root
        list.tree_nodes.each {
            if (it.name == name) {
                root = it
            }
        }
        return root
    }

    def getNodeByName(root, name) {
        if (root.name == name) {
            return root
        }
        def result
        if (root.children) {
            root.children.each {
                def node = getNodeByName(it, name)
                if (node) {
                    result = node
                }
            }
        }
        return result
    }

    def getChildrenAtributes(node, attribute) {
        def nodeNames = [node.get(attribute)]
        if (node.children) {
            node.children.each {
                nodeNames.addAll(getChildrenAtributes(it, attribute))
            }
        }
        return nodeNames
    }
}
