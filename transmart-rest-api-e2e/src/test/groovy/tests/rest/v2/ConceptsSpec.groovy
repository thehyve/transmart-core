/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static config.Config.*

/**
 * Test the endpoint for fetching concepts.
 */
class ConceptsSpec extends RESTSpec {

    /**
     *  given: "study EHR is loaded"
     *  when: "I get all concepts"
     *  then: "the 2 concepts for the study are included in the result"
     */
    @RequiresStudy(EHR_ID)
    def "get all concepts"() {
        given: "study EHR is loaded"
        def request = [
                path      : PATH_CONCEPTS,
                acceptType: JSON,
        ]

        when: "I get all concepts"
        def responseData = get(request)

        then: "the 2 concepts for the study are included in the result"
        assert responseData.concepts.size() >= 2
        assert responseData.concepts*.conceptCode.containsAll(['EHR:DEM:AGE', 'EHR:VSIGN:HR'])
    }

    /**
     *  given: "study EHR is loaded"
     *  when: "I get all concepts as admin"
     *  then: "the 2 concepts for the study are included in the result"
     */
    @RequiresStudy(EHR_ID)
    def "get all concepts as admin"() {
        given: "study EHR is loaded"
        def request = [
                path      : PATH_CONCEPTS,
                acceptType: JSON,
                user: ADMIN_USER
        ]

        when: "I get all concepts as admin"
        def responseData = get(request)

        then: "the 2 concepts for the study are included in the result"
        assert responseData.concepts.size() >= 2
        assert responseData.concepts*.conceptCode.containsAll(['EHR:DEM:AGE', 'EHR:VSIGN:HR'])
    }

    /**
     *  given: "study EHR is loaded"
     *  when: "I get the concept with code EHR:VSIGN:HR"
     *  then: "the concept is returned"
     */
    @RequiresStudy(EHR_ID)
    def "get concept by concept code"() {
        given: "study EHR is loaded"
        def request = [
                path      : "${PATH_CONCEPTS}/EHR:VSIGN:HR",
                acceptType: JSON,
        ]

        when: "I get the concept with code EHR:VSIGN:HR"
        def responseData = get(request)

        then: "the concept is returned"
        assert responseData.conceptCode == 'EHR:VSIGN:HR'
        assert responseData.name == 'Heart Rate'
    }

    /**
     *  given: "study SHARED_CONCEPTS_STUDY_C_PRIV is loaded"
     *  when: "I get the concept with code SCSCP:DEM:AGE as regular user"
     *  then: "access is denied"
     */
    @RequiresStudy(SHARED_CONCEPTS_RESTRICTED_ID)
    def "get concept by concept code for restricted data"() {
        given: "study SHARED_CONCEPTS_STUDY_C_PRIV is loaded"
        def request = [
                path      : "${PATH_CONCEPTS}/SCSCP:DEM:AGE",
                acceptType: JSON,
                statusCode: 403
        ]

        when: "I get the concept with code SCSCP:DEM:AGE as regular user"
        def responseData = get(request)

        then: "access is denied"
        assert responseData.httpStatus == 403
    }

    /**
     *  given: "study SHARED_CONCEPTS_STUDY_C_PRIV is loaded"
     *  when: "I get the concept with code SCSCP:DEM:AGE as admin"
     *  then: "the concept is returned"
     */
    @RequiresStudy(SHARED_CONCEPTS_RESTRICTED_ID)
    def "get concept by concept code for restricted data as admin"() {
        given: "study SHARED_CONCEPTS_STUDY_C_PRIV is loaded"
        def request = [
                path      : "${PATH_CONCEPTS}/SCSCP:DEM:AGE",
                acceptType: JSON,
                user: ADMIN_USER
        ]

        when: "I get the concept with code SCSCP:DEM:AGE as admin"
        def responseData = get(request)

        then: "the concept is returned"
        assert responseData.conceptCode == 'SCSCP:DEM:AGE'
        assert responseData.name == 'Age'
    }

}
