/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static config.Config.PATH_RELATION_TYPES
import static config.Config.PATH_PATIENTS
import static config.Config.SURVEY1_ID
import static tests.rest.constraints.PatientSetConstraint
import static tests.rest.constraints.RelationConstraint

/**
 * Tests for pedigree querying.
 */
class RelationSpec extends RESTSpec {

    @RequiresStudy(SURVEY1_ID)
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
                path: PATH_PATIENTS,
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
        responseData.relationTypes.size() == 6
        def mz = responseData.relationTypes.find { it.label == 'MZ' }
        mz.id == 4
        mz.biological == true
        mz.description == 'Monozygotic twin'
        mz.symmetrical == true
    }

}
