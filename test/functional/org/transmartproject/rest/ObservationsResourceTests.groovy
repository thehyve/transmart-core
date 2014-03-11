package org.transmartproject.rest

import com.grailsrocks.functionaltest.APITestCase
import org.hamcrest.Matchers

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class ObservationsResourceTests extends APITestCase {

    def study = 'STUDY1'
    void testListAllObservationsForStudy() {
        get("${baseURL}studies/${study}/observations")
        assertStatus 200

        //TODO test output
        //assertThat JSON, hasKey('observations')
    }

    void testListAllObservationsForSubject() {
        def subjectId = -102
        get("${baseURL}studies/${study}/subjects/${subjectId}/observations")
        assertStatus 200

        //TODO test output
        //assertThat JSON, hasKey('observations')
    }

    /*FIXME
    void testListAllObservationsForConcept() {
        def conceptId = 'bar'
        get("${baseURL}studies/${study}/concepts/${conceptId}/observations")
        assertStatus 200

        //TODO test output
        assertThat JSON, hasKey('observations')
    }*/

}
