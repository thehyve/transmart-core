/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec
import groovy.util.logging.Slf4j

import static base.ContentTypeFor.JSON
import static config.Config.*

/**
 *  TMPREQ-6 Building a tree where concepts are study-specific.
 *
 *  This class tests if the /v2/studies endpoint works correctly.
 */
@Slf4j
@RequiresStudy([SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID, SHARED_CONCEPTS_RESTRICTED_ID])
class StudiesSpec extends RESTSpec {

    /**
     *  given: "All studies are loaded and I do have unlimited access"
     *  when: "I try to fetch all studies"
     *  then: "the list of all studies is returned"
     */
    @RequiresStudy([EHR_ID, EHR_HIGHDIM_ID, CLINICAL_TRIAL_ID, CATEGORICAL_VALUES_ID, TUMOR_NORMAL_SAMPLES_ID, SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID, SHARED_CONCEPTS_RESTRICTED_ID])
    def "all studies are returned"() {
        given: "All studies are loaded and I do have unlimited access"
        when: "I try to fetch all studies"
        def responseData = get([
                path      : PATH_STUDIES,
                acceptType: JSON,
                user      : UNRESTRICTED_USER
        ])
        def studies = responseData.studies as List
        def studyIds = studies*.studyId as List

        then: "the list of all studies is returned"
        assert studyIds.containsAll(
                'EHR', 'EHR_HIGHDIM', 'CLINICAL_TRIAL', 'CATEGORICAL_VALUES', 'TUMOR_NORMAL_SAMPLES',
                'SHARED_CONCEPTS_STUDY_A', 'SHARED_CONCEPTS_STUDY_B', 'SHARED_CONCEPTS_STUDY_C_PRIV',
                'SHARED_HD_CONCEPTS_STUDY_A', 'SHARED_HD_CONCEPTS_STUDY_B', 'SHARED_HD_CONCEPTS_STUDY_C_PR'
        )
    }

    /**
     *  given: "All studies are loaded and I do have limited access"
     *  when: "I try to fetch all studies"
     *  then: "the list of all unrestricted studies is returned"
     */
    @RequiresStudy([EHR_ID, EHR_HIGHDIM_ID, CLINICAL_TRIAL_ID, CATEGORICAL_VALUES_ID, TUMOR_NORMAL_SAMPLES_ID, SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID, SHARED_CONCEPTS_RESTRICTED_ID])
    def "all unrestricted studies are returned"() {
        given: "All studies are loaded and I do have limited access"

        when: "I try to fetch all studies"
        def responseData = get([
                path      : PATH_STUDIES,
                acceptType: JSON,
        ])
        def studies = responseData.studies as List
        def studyIds = studies*.studyId as List

        then: "the list of all unrestricted studies is returned"

        assert !('SHARED_CONCEPTS_STUDY_C_PRIV' in studyIds)
        assert !('SHARED_HD_CONCEPTS_STUDY_C_PR' in studyIds)
    }

    /**
     *  given: "Shared concepts studies are loaded and I do have limited access"
     *  when: "I try to fetch study A by id"
     *  then: "the study object is returned"
     */
    def "study is fetched by id"() {
        given: "Shared concepts studies are loaded and I do have limited access"
        def response = get([
                path: "${PATH_STUDIES}/studyId/${SHARED_CONCEPTS_A_ID}",
                acceptType: JSON
        ])
        def shared_concepts_a_db_id = response.id

        when: "I try to fetch study A by id"
        def studyResponse = get([
                path      : "${PATH_STUDIES}/${shared_concepts_a_db_id}",
                acceptType: JSON,
        ])

        then: "the study object is returned"
        assert studyResponse.studyId == SHARED_CONCEPTS_A_ID
    }

    /**
     *  given: "Shared concepts studies are loaded and I do have limited access"
     *  when: "I try to fetch study A by id"
     *  then: "the study object is returned"
     */
    def "restricted study is fetched by id"() {
        given: "Shared concepts studies are loaded and I do have un limited access"
        def response = get([
                path: "${PATH_STUDIES}/studyId/${SHARED_CONCEPTS_RESTRICTED_ID}",
                acceptType: JSON,
                user: UNRESTRICTED_USER
        ])
        def shared_concepts_restricted_id = response.id

        when: "I try to fetch study A by id"
        def studyResponse = get([
                path      : "${PATH_STUDIES}/${shared_concepts_restricted_id}",
                acceptType: JSON,
                user      : UNRESTRICTED_USER
        ])

        then: "the study object is returned"
        assert studyResponse.studyId == SHARED_CONCEPTS_RESTRICTED_ID
    }

    /**
     *  given: "Shared concepts studies are loaded and I do have limited access"
     *  when: "I try to fetch study A by id"
     *  then: "the study object is returned"
     */
    def "access denied when restricted study is fetched by id"() {
        given: "Shared concepts studies are loaded and I do have limited access"
        def response = get([
                path: "${PATH_STUDIES}/studyId/${SHARED_CONCEPTS_RESTRICTED_ID}",
                acceptType: JSON,
                user: UNRESTRICTED_USER
        ])
        def shared_concepts_restricted_db_id = response.id

        when: "I try to fetch study A by id"
        def studyResponse = get([
                path      : "${PATH_STUDIES}/${shared_concepts_restricted_db_id}",
                acceptType: JSON,
                statusCode: 404
        ])

        then: "we don't distinguish between study not found and the user does not have access to"
        assert studyResponse.httpStatus == 404
        assert studyResponse.message == "Access denied to study or study does not exist: ${shared_concepts_restricted_db_id}"
    }

    /**
     *  given: "Shared concepts studies are loaded and I do have limited access"
     *  when: "I try to fetch study A by studyId"
     *  then: "the study object is returned"
     */
    def "study is fetched by name"() {
        given: "Shared concepts studies are loaded and I do have limited access"

        when: "I try to fetch study A by studyId"
        def studyResponse = get([
                path      : "${PATH_STUDIES}/studyId/${SHARED_CONCEPTS_A_ID}",
                acceptType: JSON,
        ])

        then: "the study object is returned"
        assert studyResponse.studyId == SHARED_CONCEPTS_A_ID
    }

}
