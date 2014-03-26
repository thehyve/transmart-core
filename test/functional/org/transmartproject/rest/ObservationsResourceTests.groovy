package org.transmartproject.rest

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class ObservationsResourceTests extends ResourceTestCase {

    def studyId = 'STUDY1'

    void testListAllObservationsForStudy() {
        get("${baseURL}studies/${studyId}/observations")
        assertStatus 200

        assertThat JSON, contains(
                allOf(
                        hasEntry(is('subject'), hasEntry('id', -103)),
                        hasEntry(is('concept'), hasEntry('conceptCode', '2')),
                        hasEntry('value', null)),
                allOf(
                        hasEntry(is('subject'), hasEntry('id', -102)),
                        hasEntry(is('concept'), hasEntry('conceptCode', '2')),
                        hasEntry('value', null)),
                allOf(
                        hasEntry(is('subject'), hasEntry('id', -101)),
                        hasEntry(is('concept'), hasEntry('conceptCode', '2')),
                        hasEntry(is('value'),
                                closeTo(10.0 as Double, 0.00001 as Double))))
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
                        hasEntry(is('concept'), allOf(
                                hasEntry('conceptCode', '2'),
                                hasEntry('conceptPath', '\\foo\\study1\\bar\\'),
                                hasEntry('label', '\\foo\\study1\\bar\\')
                        )),
                        hasEntry('value', 10.0 as Double)
                )
        )
    }

    void testListAllObservationsForConcept() {
        def conceptId = 'bar'
        get("${baseURL}studies/${studyId}/concepts/${conceptId}/observations")
        assertStatus 200

        assertThat JSON, contains(
                allOf(
                        hasEntry(is('subject'), hasEntry('id', -103)),
                        hasEntry(is('concept'), hasEntry('conceptCode', '2')),
                        hasEntry('value', null)
                ),
                allOf(
                        hasEntry(is('subject'), hasEntry('id', -102)),
                        hasEntry(is('concept'), hasEntry('conceptCode', '2')),
                        hasEntry('value', null)
                ),
                allOf(
                        hasEntry(is('subject'), hasEntry('id', -101)),
                        hasEntry(is('concept'), hasEntry('conceptCode', '2')),
                        hasEntry('value', 10.0 as Double)
                )
        )
    }

}
