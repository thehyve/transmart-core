/* Copyright © 2017 The Hyve B.V. */
package tests.rest.v2

import base.RESTSpec
import groovy.util.logging.Slf4j
import spock.lang.Requires

import static config.Config.*

/**
 *  TMPREQ-6 Building a tree where concepts are study-specific.
 *
 *  This class tests if the /v2/studies endpoint works correctly.
 */
@Slf4j
class StudiesSpec extends  RESTSpec{

    /**
     *  given: "All studies are loaded and I do have unlimited access"
     *  when: "I try to fetch all studies"
     *  then: "the list of all studies is returned"
     */
    @Requires({
        EHR_LOADED &&
        EHR_HIGHDIM_LOADED &&
        CLINICAL_TRIAL_LOADED &&
        CATEGORICAL_VALUES_LOADED &&
        TUMOR_NORMAL_SAMPLES_LOADED &&
        SHARED_CONCEPTS_LOADED &&
        SHARED_CONCEPTS_RESTRICTED_LOADED
    })
    def "all studies are returned"(){
        given: "All studies are loaded and I do have unlimited access"
        setUser(UNRESTRICTED_USERNAME, UNRESTRICTED_PASSWORD)
        when: "I try to fetch all studies"
        def responseData = get([
                path: PATH_STUDIES,
                acceptType: contentTypeForJSON,
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
    @Requires({
        EHR_LOADED &&
        EHR_HIGHDIM_LOADED &&
        CLINICAL_TRIAL_LOADED &&
        CATEGORICAL_VALUES_LOADED &&
        TUMOR_NORMAL_SAMPLES_LOADED &&
        SHARED_CONCEPTS_LOADED &&
        SHARED_CONCEPTS_RESTRICTED_LOADED
    })
    def "all unrestricted studies are returned"(){
        given: "All studies are loaded and I do have limited access"

        when: "I try to fetch all studies"
        def responseData = get([
                path: PATH_STUDIES,
                acceptType: contentTypeForJSON,
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
    @Requires({
        SHARED_CONCEPTS_LOADED &&
        SHARED_CONCEPTS_RESTRICTED_LOADED
    })
    def "study is fetched by id"(){
        given: "Shared concepts studies are loaded and I do have limited access"

        when: "I try to fetch study A by id"
        def studyResponse = get([
                path: "${PATH_STUDIES}/${SHARED_CONCEPTS_A_DB_ID}",
                acceptType: contentTypeForJSON,
        ])

        then: "the study object is returned"
        assert studyResponse.studyId == SHARED_CONCEPTS_A_ID
    }

    /**
     *  given: "Shared concepts studies are loaded and I do have limited access"
     *  when: "I try to fetch study A by id"
     *  then: "the study object is returned"
     */
    @Requires({
        SHARED_CONCEPTS_LOADED &&
                SHARED_CONCEPTS_RESTRICTED_LOADED
    })
    def "restricted study is fetched by id"(){
        given: "Shared concepts studies are loaded and I do have un limited access"
        setUser(UNRESTRICTED_USERNAME, UNRESTRICTED_PASSWORD)

        when: "I try to fetch study A by id"
        def studyResponse = get([
                path: "${PATH_STUDIES}/${SHARED_CONCEPTS_RESTRICTED_DB_ID}",
                acceptType: contentTypeForJSON,
        ])

        then: "the study object is returned"
        assert studyResponse.studyId == SHARED_CONCEPTS_RESTRICTED_ID
    }

    /**
     *  given: "Shared concepts studies are loaded and I do have limited access"
     *  when: "I try to fetch study A by id"
     *  then: "the study object is returned"
     */
    @Requires({
        SHARED_CONCEPTS_LOADED &&
                SHARED_CONCEPTS_RESTRICTED_LOADED
    })
    def "access denied when restricted study is fetched by id"(){
        given: "Shared concepts studies are loaded and I do have limited access"

        when: "I try to fetch study A by id"
        def studyResponse = get([
                path: "${PATH_STUDIES}/${SHARED_CONCEPTS_RESTRICTED_DB_ID}",
                acceptType: contentTypeForJSON,
                statusCode: 403
        ])

        then: "the study object is returned"
        assert studyResponse.httpStatus == 403
        assert studyResponse.message == "Access denied to study or study does not exist: ${SHARED_CONCEPTS_RESTRICTED_DB_ID}"
    }

    /**
     *  given: "Shared concepts studies are loaded and I do have limited access"
     *  when: "I try to fetch study A by studyId"
     *  then: "the study object is returned"
     */
    @Requires({
        SHARED_CONCEPTS_LOADED &&
        SHARED_CONCEPTS_RESTRICTED_LOADED
    })
    def "study is fetched by name"(){
        given: "Shared concepts studies are loaded and I do have limited access"

        when: "I try to fetch study A by studyId"
        def studyResponse = get([
                path: "${PATH_STUDIES}/studyId/${SHARED_CONCEPTS_A_ID}",
                acceptType: contentTypeForJSON,
        ])

        then: "the study object is returned"
        assert studyResponse.studyId == SHARED_CONCEPTS_A_ID
    }

}
