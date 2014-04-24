package org.transmartproject.rest

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.thehyve.commons.test.FastMatchers.listOfWithOrder

class ObservationsResourceTests extends ResourceTestCase {

    def studyId = 'STUDY1'
    def label = "\\foo\\study1\\bar\\"

    def study1BarExpectedObservations = [
            [
                    subject: [id: -103],
                    label: label,
                    value: null,
            ],
            [
                    subject: [id: -102],
                    label: label,
                    value: null,
            ],
            [
                    subject: [id: -101],
                    label: label,
                    value: closeTo(10.0 as Double, 0.00001 as Double),
            ],
    ]

    void testListAllObservationsForStudy() {
        get("${baseURL}studies/${studyId}/observations")
        assertStatus 200

        assertThat JSON, listOfWithOrder(study1BarExpectedObservations)
    }

    void testListAllObservationsForSubject() {
        def subjectId = -101
        get("${baseURL}studies/${studyId}/subjects/${subjectId}/observations")
        assertStatus 200

        assertThat JSON, contains(
                allOf(
                        hasEntry(is('subject'), allOf(
                                hasEntry('id', subjectId),
                                hasEntry('sex', 'UNKOWN'),
                                hasEntry('trial', studyId),
                                hasEntry('inTrialId', 'SUBJ_ID_1'),
                                hasEntry('religion', null),
                                hasEntry('age', null),
                                hasEntry('birthDate', null),
                                hasEntry('maritalStatus', null),
                                hasEntry('deathDate', null),
                                hasEntry('race', null),
                        )),

                        hasEntry('value', 10.0 as Double)
                )
        )
    }

    void testListAllObservationsForConcept() {
        def conceptId = 'bar'
        get("${baseURL}studies/${studyId}/concepts/${conceptId}/observations")
        assertStatus 200

        assertThat JSON, listOfWithOrder(study1BarExpectedObservations)
    }
}
