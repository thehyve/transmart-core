/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2.hypercube

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static base.ContentTypeFor.PROTOBUF
import static config.Config.*
import static tests.rest.Operator.AND
import static tests.rest.Operator.OR
import static tests.rest.constraints.*

/**
 *  TMPREQ-5 Building a generic concept tree across studies
 */
@RequiresStudy([SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID])
class SharedConceptsSpec extends RESTSpec {

    /**
     *  given: "studies STUDIENAME and STUDIENAME are loaded and both use shared Consept ids"
     *  when: "I get observaties using this shared Consept id"
     *  then: "observations are returned from both Studies"
     */
    def "get shared concept multi study"() {
        given: "studies STUDIENAME and STUDIENAME are loaded and both use shared Consept ids"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"])
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
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

    /**
     *  given: "studies SHARED_CONCEPTS_A and SHARED_CONCEPTS_B are loaded and both use shared Consept ids"
     *  when: "I get observaties of one study using this shared Consept id"
     *  then: "observations are returned from only that Studies"
     */
    def "get shared concept single study"() {
        given: "studies SHARED_CONCEPTS_A and SHARED_CONCEPTS_B are loaded and both use shared Consept ids"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type    : Combination,
                        operator: AND,
                        args    : [
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
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

    /**
     *  given: "studies SHARED_CONCEPTS_A, SHARED_CONCEPTS_B and SHARED_CONCEPTS_RESTRICTED are loaded and I do not have access"
     *  when: "I get observaties using a shared Consept id"
     *  then: "observations are returned from both public Studies but not the restricted study"
     */
    @RequiresStudy(SHARED_CONCEPTS_RESTRICTED_ID)
    def "get shared concept restricted"() {
        given: "studies STUDIENAME, STUDIENAME and STUDIENAME_RESTRICTED are loaded and all use shared Consept ids"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"])
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
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

    /**
     *  given: "studies SHARED_CONCEPTS_A, SHARED_CONCEPTS_B and SHARED_CONCEPTS_RESTRICTED are loaded and I do not have access"
     *  when: "I get observaties using a shared Consept id"
     *  then: "observations are returned from both public Studies but not the restricted study"
     */
    @RequiresStudy(SHARED_CONCEPTS_RESTRICTED_ID)
    def "get shared concept unrestricted"() {
        given: "studies STUDIENAME, STUDIENAME and STUDIENAME_RESTRICTED are loaded and all use shared Consept ids"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"]),
                user      : UNRESTRICTED_USERNAME
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
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

    @RequiresStudy(SHARED_CONCEPTS_RESTRICTED_ID)
    def "limit shared concept"() {
        def heartRate = [type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"]

        def studiesOR = [
                type    : Combination,
                operator: OR,
                args    : [
                        [type: StudyNameConstraint, studyId: SHARED_CONCEPTS_A_ID],
                        [type: StudyNameConstraint, studyId: SHARED_CONCEPTS_B_ID]
                ]
        ]

        def constaint = [
                type    : Combination,
                operator: AND,
                args    : [
                        studiesOR,
                        heartRate
                ]
        ]

        when:
        def responseData = get([path: PATH_OBSERVATIONS, acceptType: acceptType, query: toQuery(constaint), user: ADMIN_USERNAME])
        def selector = newSelector(responseData)

        then:
        (0..<selector.cellCount).each {
            assert [SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID, SHARED_CONCEPTS_RESTRICTED_ID].contains(selector.select(it, "study", "name", 'String'))
            assert selector.select(it, "concept", "conceptCode", 'String').equals('VSIGN:HR')
            assert selector.select(it) != null
        }

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }
}