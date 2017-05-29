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
        
        then: "the list of trial visits for unrestricted studies is returned"
        elements.size() == 19
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
                
        then: "the list of trial visits for all studies is returned"
        elements.size() == 21
    }
}
