/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v1

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static config.Config.*

@RequiresStudy(GSE8581_ID)
class SubjectsSpec extends RESTSpec {

    /**
     *  given: "study EHR is loaded"
     *  when: "I request subjects related to this study"
     *  then: "subjects are returned"
     */
    def "v1 subjects"() {
        given: "study EHR is loaded"
        def studyId = GSE8581_ID

        when: "I request subjects related to this study"
        def responseData = get([
                path      : V1_PATH_STUDIES + "/${studyId}/subjects",
                acceptType: JSON,
                user      : ADMIN_USERNAME
        ])

        then: "subjects are returned"
        assert responseData.subjects.size() == 58
        responseData.subjects.each {
            assert it.age != null
            assert it.birthDate == null
            assert it.deathDate == null
            assert it.id != null
            assert it.inTrialId.contains('GSE8581')
            assert it.maritalStatus == null
            assert ['Caucasian', 'Afro American'].contains(it.race)
            assert it.religion == null
            assert ['FEMALE', 'MALE', 'UNKNOWN'].contains(it.sex)
            assert it.trial == 'GSE8581'
        }
    }

    /**
     *  given: "study EHR is loaded"
     *  when: "I request a subject by it's id"
     *  then: "only that subject is returned"
     */
    def "v1 single subject"() {
        given: "study EHR is loaded"
        def studyId = GSE8581_ID

        when: "I request a subject by it's id"
        def subjectId = get([
                path      : V1_PATH_STUDIES + "/${studyId}/subjects",
                acceptType: JSON,
                user      : ADMIN_USERNAME
        ]).subjects[0].id

        def responseData = get([
                path      : V1_PATH_STUDIES + "/${studyId}/subjects/${subjectId}",
                acceptType: JSON,
                user      : ADMIN_USERNAME
        ])

        then: "only that subject is returned"
        assert responseData.age != null
        assert responseData.birthDate == null
        assert responseData.deathDate == null
        assert responseData.id == subjectId
        assert responseData.inTrialId.contains('GSE8581')
        assert responseData.maritalStatus == null
        assert ['Caucasian', 'Afro American'].contains(responseData.race)
        assert responseData.religion == null
        assert ['FEMALE', 'MALE', 'UNKNOWN'].contains(responseData.sex)
        assert responseData.trial == 'GSE8581'
    }

    /**
     *  given: "study EHR is loaded"
     *  when: "I request subjects related to this study and a concept path"
     *  then: "subjects are returned"
     */
    def "v1 subjects by concept"() {
        given: "study EHR is loaded"
        def studyId = GSE8581_ID
        def conceptPath = "Subjects/Age/"

        when: "I request subjects related to this study and a concept path"
        def responseData = get([
                path      : V1_PATH_STUDIES + "/${studyId}/concepts/${conceptPath}/subjects",
                acceptType: JSON,
                user      : ADMIN_USERNAME
        ])

        then: "subjects are returned"
        assert responseData.subjects.size() == 58
        responseData.subjects.each {
            assert it.age != null
            assert it.birthDate == null
            assert it.deathDate == null
            assert it.id != null
            assert it.inTrialId.contains('GSE8581')
            assert it.maritalStatus == null
            assert ['Caucasian', 'Afro American'].contains(it.race)
            assert it.religion == null
            assert ['FEMALE', 'MALE', 'UNKNOWN'].contains(it.sex)
            assert it.trial == 'GSE8581'
        }
    }
}
