package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec
import groovy.util.logging.Slf4j

import static base.ContentTypeFor.contentTypeForJSON
import static config.Config.*

@Slf4j
class DimensionSpec extends RESTSpec{
    
    @RequiresStudy([EHR_ID, EHR_HIGHDIM_ID, CLINICAL_TRIAL_ID, CATEGORICAL_VALUES_ID, TUMOR_NORMAL_SAMPLES_ID, SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID, SHARED_CONCEPTS_RESTRICTED_ID])
    def "Trial visit dimension elements are returned for all unrestricted studies"() {
        given: "All studies are loaded and I do have unlimited access"
        
        when: "I try to fetch all trial visits"
        def dimensionName = "trial visit"
        def responseData = get([
                path      : PATH_DIMENSION + "/$dimensionName",
                acceptType: contentTypeForJSON
        ])
        def elements = responseData.elements
        def studyIds = elements*.study as List
        
        then: "the list of trial visits for unrestricted studies is returned"
        assert !('SHARED_CONCEPTS_STUDY_C_PRIV' in studyIds)
        assert !('SHARED_HD_CONCEPTS_STUDY_C_PR' in studyIds)
    }
    
    @RequiresStudy([EHR_ID, EHR_HIGHDIM_ID, CLINICAL_TRIAL_ID, CATEGORICAL_VALUES_ID, TUMOR_NORMAL_SAMPLES_ID, SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID, SHARED_CONCEPTS_RESTRICTED_ID])
    def "Trial visit dimension elements are returned for all studies"() {
        given: "All studies are loaded and I do have unlimited access"
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        
        when: "I try to fetch all trial visits"
        def dimensionName = "trial visit"
        def responseData = get([
                path      : PATH_DIMENSION + "/$dimensionName",
                acceptType: contentTypeForJSON
        ])
        def elements = responseData.elements
        def studyIds = elements*.study as List
        
        then: "the list of all trial visits is returned"
        assert studyIds.containsAll(
                'EHR', 'EHR_HIGHDIM', 'CLINICAL_TRIAL', 'CATEGORICAL_VALUES', 'TUMOR_NORMAL_SAMPLES',
                'SHARED_CONCEPTS_STUDY_A', 'SHARED_CONCEPTS_STUDY_B', 'SHARED_CONCEPTS_STUDY_C_PRIV',
                'SHARED_HD_CONCEPTS_STUDY_A', 'SHARED_HD_CONCEPTS_STUDY_B', 'SHARED_HD_CONCEPTS_STUDY_C_PR'
        )
    }
}
