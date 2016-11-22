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

@IgnoreIf({SUPPRESS_UNIMPLEMENTED})
class GetTreeNodesSpec extends  RESTSpec{

    /**
     *  given: "Study SHARED_CONCEPTS_STUDY_C_PRIV and SHARED_CONCEPTS_A is loaded, and I have access"
     *  when: "I try to get the tree_nodes from all studies"
     *  then: "nodes from SHARED_CONCEPTS_STUDY_C_PRIV are included"
     */
    @Requires({SHARED_CONCEPTS_RESTRICTED_LOADED && SHARED_CONCEPTS_LOADED})
    def "restricted tree_nodes are included"(){
        given: "Study SHARED_CONCEPTS_STUDY_C_PRIV and SHARED_CONCEPTS_A is loaded, and I do not have access"
        setUser(UNRESTRICTED_USERNAME, UNRESTRICTED_PASSWORD)

        when: "I try to get the tree_nodes from all studies"
        def responseData = get('tree_nodes', contentTypeForJSON)

        then: "only nodes from SHARED_CONCEPTS_A are returned"
        that responseData, hasEntry(is('tree_nodes'),
                hasItem(hasEntry(is('key'), containsString('\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\')))
        )
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
        def responseData = get('tree_nodes', contentTypeForJSON)

        then: "only nodes from SHARED_CONCEPTS_A are returned"


        that responseData, hasEntry(is('tree_nodes'),
                everyItem(hasEntry(is('key'), not(containsString('\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\'))))
        )
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
        def queryMap = [parent_key : '\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\', depth : 2]
        def responseData = get('tree_nodes', contentTypeForJSON)

        then: "I get an access error"
        assert responseData.httpStatus == 403
        assert responseData.type == 'AccessDeniedException'
        assert responseData.message == "Access denied to study: ${SHARED_CONCEPTS_RESTRICTED_ID}"
    }

    def "get tree_nodes as json"(){
        when: "when I do a GET '/tree_nodes' with header Accept: ${contentTypeForJSON}"
        def responseData = get('tree_nodes', contentTypeForJSON)

        then: 'then I get all studies in a json format'
        that responseData, hasEntry(is('tree_nodes'), hasItems(
                allOf(
                        hasEntry('key', "\\\\i2b2\\Studies\\GSE8581\\Subjects\\"),
                        hasEntry('parent_key', "\\\\i2b2\\Studies\\GSE8581\\"),
                        hasEntry('name', "Subjects"),
                        hasEntry(is('visual_attributes'), contains("FOLDER")),
                        hasEntry('patient_count', 15)
                ),
                allOf(
                        hasEntry('key', "\\\\i2b2\\Studies\\GSE8581\\Subjects\\Age\\"),
                        hasEntry('parent_key', "\\\\i2b2\\Studies\\GSE8581\\Subjects\\"),
                        hasEntry('name', "Age"),
                        hasEntry(is('visual_attributes'), contains("NUMERIC")),
                        hasEntry('patient_count', 15)
                )
        ))
    }

    def "get tree_nodes as json with query"(){
        def queryMap = [parent_key : "\\\\i2b2\\Studies\\GSE8581\\", depth : 2]

        when: "when I do a GET '/tree_nodes' with header Accept: ${contentTypeForJSON}"
        def responseData = get('tree_nodes', contentTypeForJSON, queryMap)

        then: 'then I get all studies in a json format'
        that responseData, hasEntry(is('tree_nodes'), hasItems(
                allOf(
                        hasEntry('key', "\\\\i2b2\\Studies\\GSE8581\\Subjects\\"),
                        hasEntry('parent_key', "\\\\i2b2\\Studies\\GSE8581\\"),
                        hasEntry('name', "Subjects"),
                        hasEntry(is('visual_attributes'), contains("FOLDER")),
                        hasEntry('patient_count', 15)
                ),
                allOf(
                        hasEntry('key', "\\\\i2b2\\Studies\\GSE8581\\Subjects\\Age\\"),
                        hasEntry('parent_key', "\\\\i2b2\\Studies\\GSE8581\\Subjects\\"),
                        hasEntry('name', "Age"),
                        hasEntry(is('visual_attributes'), contains("NUMERIC")),
                        hasEntry('patient_count', 15)
                )
        ))
    }

    def "get tree_nodes as Hal+json with query"(){
        def queryMap = [parent_key : "\\\\i2b2\\Studies\\GSE8581\\", depth : 2]

        when: "when I do a GET '/tree_nodes' with header Accept: ${contentTypeForHAL}"
        def responseData = get('tree_nodes', contentTypeForHAL, queryMap)

        then: 'then I get all studies in a Hal+json format'
        that responseData, halIndexResponse('/tree_nodes?parent_key=%5C%5Ci2b2%5CStudies%5CGSE8581%5C&depth=2', ['tree_nodes':
                                                                 hasItems(
                                                                         allOf(
                                                                                 hasEntry('key', "\\\\i2b2\\Studies\\GSE8581\\Subjects\\"),
                                                                                 hasEntry('parent_key', "\\\\i2b2\\Studies\\GSE8581\\"),
                                                                                 hasEntry('name', "Subjects"),
                                                                                 hasEntry(is('visual_attributes'), contains("FOLDER")),
                                                                                 hasEntry('patient_count', 15),
                                                                                 hasChildrenLink("/tree_nodes?parent_key=%5C%5Ci2b2%5CStudies%5CGSE8581%5CSubjects%5C&depth=1")
                                                                         ),
                                                                         allOf(
                                                                                 hasEntry('key', "\\\\i2b2\\Studies\\GSE8581\\Subjects\\Age\\"),
                                                                                 hasEntry('parent_key', "\\\\i2b2\\Studies\\GSE8581\\Subjects\\"),
                                                                                 hasEntry('name', "Age"),
                                                                                 hasEntry(is('visual_attributes'), contains("NUMERIC")),
                                                                                 hasEntry('patient_count', 15),
                                                                                 hasEntry(is('_links'), emptyIterable())
                                                                         ),
                                                                 )
        ])
    }

    def jsonTestData(){
        return JSON.parse("{\n" +
                "  \"tree_nodes\": \n" +
                "  [\n" +
                "     {\n" +
                "        \"key\": \"\\\\\\\\i2b2\\\\Studies\\\\GSE8581\\\\Subjects\\\\\",\n" +
                "        \"parent_key\": \"\\\\\\\\i2b2\\\\Studies\\\\GSE8581\\\\\",\n" +
                "        \"name\": \"Subjects\",\n" +
                "        \"visual_attributes\": [ \"FOLDER\" ],\n" +
                "        \"patient_count\": 15\n" +
                "     },\n" +
                "     {\n" +
                "        \"key\": \"\\\\\\\\i2b2\\\\Studies\\\\GSE8581\\\\Subjects\\\\Age\\\\\",\n" +
                "        \"parent_key\": \"\\\\\\\\i2b2\\\\Studies\\\\GSE8581\\\\Subjects\\\\\",\n" +
                "        \"name\": \"Age\",\n" +
                "        \"visual_attributes\": [ \"NUMERIC\" ],\n" +
                "        \"patient_count\": 15\n" +
                "     }\n" +
                "   ]\n" +
                "}")
    }

    def halTestData(){
        return JSON.parse("{\n" +
                "    \"_links\": {\n" +
                "        \"self\": {\n" +
                "          \"href\": \"/tree_nodes?parent_key=%5C%5Ci2b2%5CStudies%5CGSE8581%5C&depth=2\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"_embedded\": {\n" +
                "        \"tree_nodes\": [\n" +
                "             {\n" +
                "                \"key\": \"\\\\\\\\i2b2\\\\Studies\\\\GSE8581\\\\Subjects\\\\\",\n" +
                "                \"parent_key\": \"\\\\\\\\i2b2\\\\Studies\\\\GSE8581\\\\\",\n" +
                "                \"name\": \"Subjects\",\n" +
                "                \"visual_attributes\": [ \"FOLDER\" ],\n" +
                "                \"patient_count\": 15,\n" +
                "                \"_links\": {\n" +
                "                  \"children\": {\n" +
                "                    \"href\": \"/tree_nodes?parent_key=%5C%5Ci2b2%5CStudies%5CGSE8581%5CSubjects%5C&depth=1\"\n" +
                "                  }\n" +
                "                }\n" +
                "             },\n" +
                "             {\n" +
                "                \"key\": \"\\\\\\\\i2b2\\\\Studies\\\\GSE8581\\\\Subjects\\\\Age\\\\\",\n" +
                "                \"parent_key\": \"\\\\\\\\i2b2\\\\Studies\\\\GSE8581\\\\Subjects\\\\\",\n" +
                "                \"name\": \"Age\",\n" +
                "                \"visual_attributes\": [ \"NUMERIC\" ],\n" +
                "                \"patient_count\": 15,\n" +
                "                \"_links\": {}\n" +
                "            },\n" +
                "        ]\n" +
                "    }\n" +
                "}")
    }
}
