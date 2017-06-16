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
                path      : PATH_DIMENSION + "/$dimensionName/elements",
                acceptType: contentTypeForJSON
        ])

        then: "the list of trial visits for unrestricted studies is returned"
        responseData.size() == 19
    }
    
    @RequiresStudy([EHR_ID, EHR_HIGHDIM_ID, CLINICAL_TRIAL_ID, CATEGORICAL_VALUES_ID, TUMOR_NORMAL_SAMPLES_ID, SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID, SHARED_CONCEPTS_RESTRICTED_ID])
    def "Trial visit dimension elements are returned for all studies"() {
        given: "All studies are loaded and I do have unlimited access"
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        
        when: "I try to fetch all trial visits"
        def dimensionName = "trial visit"
        def responseData = get([
                path      : PATH_DIMENSION + "/$dimensionName/elements",
                acceptType: contentTypeForJSON
        ])

        then: "the list of trial visits for all studies is returned"
        responseData.size() == 21
    }

    def "Invalid dimension name"() {
        when: "I try to fetch dimension with invaid name"
        def dimensionName = "invalid name"
        def responseData = get([
                path      : PATH_DIMENSION + "/$dimensionName/elements",
                acceptType: contentTypeForJSON,
                statusCode: 400
        ])

        then: "Exception is thrown"
        assert responseData.httpStatus == 400
        assert responseData.type == 'InvalidArgumentsException'
        assert responseData.message == "Dimension with a name '$dimensionName' is invalid."
    }
}
