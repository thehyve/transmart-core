/* Copyright © 2017 The Hyve B.V. */
package tests.rest.v2.hypercube

import base.RESTSpec
import spock.lang.Requires

import static config.Config.*
import static tests.rest.v2.Operator.AND
import static tests.rest.v2.Operator.OR
import static tests.rest.v2.constraints.*

/**
 *  TMPREQ-5 Building a generic concept tree across studies
 */
@Requires({SHARED_CONCEPTS_LOADED})
class SharedConceptsSpec extends RESTSpec {

    /**
     *  given: "studies STUDIENAME and STUDIENAME are loaded and both use shared Consept ids"
     *  when: "I get observaties using this shared Consept id"
     *  then: "observations are returned from both Studies"
     */
    def "get shared concept multi study"(){
        given: "studies STUDIENAME and STUDIENAME are loaded and both use shared Consept ids"
        def request = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: toQuery([type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"])
        ]

        when: "I get observaties using this shared Consept id"
        def responseData = get(request)
        def selector = newSelector(responseData)

        then: "observations are returned from both Studies"
        (0..<selector.cellCount).each {
            assert [SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID].contains(selector.select(it, "study", "name", 'String'))
            assert selector.select(it, "concept", "conceptCode", 'String').equals('VSIGN:HR')
            assert selector.select(it) != null
        }

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "studies SHARED_CONCEPTS_A and SHARED_CONCEPTS_B are loaded and both use shared Consept ids"
     *  when: "I get observaties of one study using this shared Consept id"
     *  then: "observations are returned from only that Studies"
     */
    def "get shared concept single study"(){
        given: "studies SHARED_CONCEPTS_A and SHARED_CONCEPTS_B are loaded and both use shared Consept ids"
        def request = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: toQuery([
                        type: Combination,
                        operator: AND,
                        args: [
                                [type: StudyNameConstraint, studyId: SHARED_CONCEPTS_A_ID],
                                [type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"]
                        ]
                ])
        ]

        when: "I get observaties of one study using this shared Consept id"
        def responseData = get(request)
        def selector = newSelector(responseData)

        then: "observations are returned from only that Studies"
        (0..<selector.cellCount).each {
            assert selector.select(it, "study", "name", 'String').equals(SHARED_CONCEPTS_A_ID)
            assert selector.select(it, "concept", "conceptCode", 'String').equals('VSIGN:HR')
            assert selector.select(it) != null
        }

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "studies SHARED_CONCEPTS_A, SHARED_CONCEPTS_B and SHARED_CONCEPTS_RESTRICTED are loaded and I do not have access"
     *  when: "I get observaties using a shared Consept id"
     *  then: "observations are returned from both public Studies but not the restricted study"
     */
    @Requires({SHARED_CONCEPTS_RESTRICTED_LOADED})
    def "get shared concept restricted"(){
        given: "studies STUDIENAME, STUDIENAME and STUDIENAME_RESTRICTED are loaded and all use shared Consept ids"
        def request = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: toQuery([type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"])
        ]

        when: "I get observaties using this shared Consept id"
        def responseData = get(request)
        def selector = newSelector(responseData)

        then: "observations are returned from both public Studies but not the restricted study"
        (0..<selector.cellCount).each {
            assert [SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID].contains(selector.select(it, "study", "name", 'String'))
            assert !selector.select(it, "study", "name", 'String').equals(SHARED_CONCEPTS_RESTRICTED_ID)
            assert selector.select(it, "concept", "conceptCode", 'String').equals('VSIGN:HR')
            assert selector.select(it) != null
        }

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "studies SHARED_CONCEPTS_A, SHARED_CONCEPTS_B and SHARED_CONCEPTS_RESTRICTED are loaded and I do not have access"
     *  when: "I get observaties using a shared Consept id"
     *  then: "observations are returned from both public Studies but not the restricted study"
     */
    @Requires({SHARED_CONCEPTS_RESTRICTED_LOADED})
    def "get shared concept unrestricted"(){
        given: "studies STUDIENAME, STUDIENAME and STUDIENAME_RESTRICTED are loaded and all use shared Consept ids"
        setUser(UNRESTRICTED_USERNAME, UNRESTRICTED_PASSWORD)
        def request = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: toQuery([type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"])
        ]

        when: "I get observaties using this shared Consept id"
        def responseData = get(request)
        def selector = newSelector(responseData)

        then: "observations are returned from both public Studies but not the restricted study"
        (0..<selector.cellCount).each {
            assert [SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID, SHARED_CONCEPTS_RESTRICTED_ID].contains(selector.select(it, "study", "name", 'String'))
            assert selector.select(it, "concept", "conceptCode", 'String').equals('VSIGN:HR')
            assert selector.select(it) != null
        }

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    def "limit shared concept"(){
        setUser(ADMIN_USERNAME,ADMIN_PASSWORD)
        def heartRate = [type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"]

        def studiesOR = [
                type: Combination,
                operator: OR,
                args: [
                        [type: StudyNameConstraint, studyId: SHARED_CONCEPTS_A_ID],
                        [type: StudyNameConstraint, studyId: SHARED_CONCEPTS_B_ID]
                ]
        ]

        def constaint = [
                type: Combination,
                operator: AND,
                args: [
                        studiesOR,
                        heartRate
                ]
        ]

        when:
        def responseData = get([path: PATH_OBSERVATIONS, acceptType: acceptType, query: toQuery(constaint)])
        def selector = newSelector(responseData)

        then:
        (0..<selector.cellCount).each {
            assert [SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID, SHARED_CONCEPTS_RESTRICTED_ID].contains(selector.select(it, "study", "name", 'String'))
            assert selector.select(it, "concept", "conceptCode", 'String').equals('VSIGN:HR')
            assert selector.select(it) != null
        }

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }
}