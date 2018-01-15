/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static config.Config.*
import static tests.rest.constraints.PatientSetConstraint
import static tests.rest.constraints.RelationConstraint

/**
 * Tests for pedigree querying.
 */
@RequiresStudy(SURVEY1_ID)
class RelationSpec extends RESTSpec {


    def "get patients based on relations"() {
        given: "study SURVEY1 is loaded"
        def params = [
                constraint: toJSON([
                        type                     : RelationConstraint,
                        relationTypeLabel        : 'PAR',
                        relatedSubjectsConstraint: [type: PatientSetConstraint, subjectIds: ['7']],
                ]),
        ]
        def request = [
                path      : PATH_PATIENTS,
                acceptType: JSON,
        ]

        when: "I get subjects that are parents of subject with id 7."
        def responseData = getOrPostRequest(method, request, params)

        then: "2 parents are returned"
        responseData.patients.size() == 2
        responseData.patients*.id as Set == [-3001, -3002] as Set

        where:
        method | _
        "POST" | _
        "GET"  | _
    }

    def "get relation types"() {
        when:
        def responseData = get([
                path      : PATH_RELATION_TYPES,
                acceptType: JSON,
        ])
        then:
        responseData.relationTypes.size() == 7
        def mz = responseData.relationTypes.find { it.label == 'MZ' }
        mz.id == 4
        mz.biological == true
        mz.description == 'Monozygotic twin'
        mz.symmetrical == true
    }

    def "get observations of the parents of patient with id -3007"() {
        given: "study SURVEY1 is loaded"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: JSON,
                query     : toQuery([
                        type                     : "relation",
                        relatedSubjectsConstraint: ["type": "patient_set", patientIds: [-3007]],
                        "relationTypeLabel"      : "PAR",
                        "biological"             : true,
                        "shareHousehold"         : true
                ]),
        ]

        when: "I get observations of the parents of patient with id -3007."

        def responseData = get(request)
        def selector = jsonSelector(responseData)

        then: "there are 16 observations"
        assert selector.cellCount == 16

    }

    def "get male parent of a twin"() {
        given: "study SURVEY1 is loaded"
        def params = [constraint: toJSON(
                [type: 'and', args: [[type: "subselection", "dimension": "patient", "constraint": ["type": "relation", "relatedSubjectsConstraint": ["type": "or", "args": [["type": "relation", "relatedSubjectsConstraint": ["type": "true"], "relationTypeLabel": "MZ", "biological": true], ["type": "relation", "relatedSubjectsConstraint": ["type": "true"], "relationTypeLabel": "DZ", "biological": true], ["type": "relation", "relatedSubjectsConstraint": ["type": "true"], "relationTypeLabel": "COT", "biological": true]]], "relationTypeLabel": "PAR"]], ["type": "subselection", "dimension": "patient", "constraint": ["type": "and", "args": [["type": "concept", "conceptCode": "gender", "name": "Gender", "fullName": "\\\\Demographics\\\\Gender\\\\", "conceptPath": "\\\\Demographics\\\\Gender\\\\", "valueType": "CATEGORICAL"], ["type": "value", "valueType": "STRING", "operator": "=", "value": "Male"]]]]]]
        ),
        ]
        def request = [
                path      : PATH_PATIENTS,
                acceptType: JSON,
        ]

        when: "I get subjects that are parents of subject with id 7."
        def responseData = getOrPostRequest(method, request, params)

        then: "2 parents are returned"
        responseData.patients.size() == 2
        responseData.patients*.id as Set == [-3001, -3005] as Set

        where:
        method | _
        "POST" | _
        "GET"  | _
    }
}
