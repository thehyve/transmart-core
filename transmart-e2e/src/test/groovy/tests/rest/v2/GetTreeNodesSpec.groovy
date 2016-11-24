package tests.rest.v2

import grails.converters.JSON
import org.hamcrest.core.StringContains
import spock.lang.Ignore
import spock.lang.IgnoreIf
import spock.lang.Requires

import static config.Config.*
import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that
import base.RESTSpec

class GetTreeNodesSpec extends  RESTSpec{

    /**
     *  given: "Study SHARED_CONCEPTS_STUDY_C_PRIV and SHARED_CONCEPTS_A is loaded, and I have access"
     *  when: "I try to get the tree_nodes from all studies"
     *  then: "nodes from SHARED_CONCEPTS_STUDY_C_PRIV and SHARED_CONCEPTS_A are included"
     */
    @Requires({SHARED_CONCEPTS_RESTRICTED_LOADED && SHARED_CONCEPTS_LOADED})
    def "restricted tree_nodes are included"(){
        given: "Study SHARED_CONCEPTS_STUDY_C_PRIV and SHARED_CONCEPTS_A is loaded, and I do not have access"
        setUser(UNRESTRICTED_USERNAME, UNRESTRICTED_PASSWORD)

        when: "I try to get the tree_nodes from all studies"
        def responseData = get(PATH_TREE_NODES, contentTypeForJSON)

        then: "only nodes from SHARED_CONCEPTS_A are returned"
        def studyA = getRootNodeByName(responseData, SHARED_CONCEPTS_A_ID)
        def studyC = getRootNodeByName(responseData, SHARED_CONCEPTS_RESTRICTED_ID)

        assert getChildren(studyA, 'name').containsAll([SHARED_CONCEPTS_A_ID, 'Vital Signs', 'Heart Rate', 'Demography', 'Age'])
        assert getChildren(studyC, 'name').containsAll([SHARED_CONCEPTS_RESTRICTED_ID, 'Vital Signs', 'Heart Rate', 'Demography', 'Age'])
    }



    /**
     *  given: "Study SHARED_CONCEPTS_STUDY_C_PRIV and SHARED_CONCEPTS_A is loaded, and I do not have access"
     *  when: "I try to get the tree_nodes from all studies"
     *  then: "only nodes from SHARED_CONCEPTS_A are returned"
     */
    @Requires({SHARED_CONCEPTS_RESTRICTED_LOADED && SHARED_CONCEPTS_LOADED})
    def "restricted tree_nodes are excluded"(){
        given: "Study SHARED_CONCEPTS_STUDY_C_PRIV and SHARED_CONCEPTS_A is loaded, and I do not have access"

        when: "I try to get the tree_nodes from all studies"
        def responseData = get(PATH_TREE_NODES, contentTypeForJSON)

        then: "only nodes from SHARED_CONCEPTS_A are returned"
        def studyA = getRootNodeByName(responseData, SHARED_CONCEPTS_A_ID)
        def studyC = getRootNodeByName(responseData, SHARED_CONCEPTS_RESTRICTED_ID)

        assert getChildren(studyA, 'name').containsAll([SHARED_CONCEPTS_A_ID, 'Vital Signs', 'Heart Rate', 'Demography', 'Age'])
        assert studyC == null
    }

    /**
     *  given: "Study SHARED_CONCEPTS is loaded"
     *  when: "I get the tree_nodes with a subNode and depth"
     *  then: "only de subNodes of that node are returned"
     */
    @Requires({SHARED_CONCEPTS_LOADED})
    def "params limit nodes returned"(){
        given: "Study SHARED_CONCEPTS is loaded"

        when: "I get the tree_nodes with a subNode and depth"

        then: "only de subNodes of that node are returned"
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
     *  when: "I try to get the tree_nodes from that study"
     *  then: "I get an access error"
     */
    @Requires({SHARED_CONCEPTS_RESTRICTED_LOADED})
    def "restricted tree_nodes"() {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"

        when: "I try to get the tree_nodes from that study"
        def queryMap = [parent_key : "\\Public Studies\\CLINICAL_TRIAL\\", depth : 2]
        def responseData = get(PATH_TREE_NODES, contentTypeForJSON, queryMap)

        then: "I get an access error"
        assert responseData.httpStatus == 403
        assert responseData.type == 'AccessDeniedException'
        assert responseData.message == "Access denied to study: ${SHARED_CONCEPTS_RESTRICTED_ID}"
    }

    def getRootNodeByName(list, name){
        def root
        list.tree_nodes.each{
            if (it.name == name){
                root = it
            }
        }
        return root
    }

    def getChildren(node, attribute){
        def nodeNames = [node.get(attribute)]
        if (node.children){
            node.children.each{
                nodeNames.addAll(getChildren(it, attribute))
            }
        }
        return nodeNames
    }
}
