package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec
import groovy.util.logging.Slf4j

import static base.ContentTypeFor.JSON
import static config.Config.*

@Slf4j
class DimensionSpec extends RESTSpec {

    @RequiresStudy([EHR_ID, EHR_HIGHDIM_ID, CLINICAL_TRIAL_ID, CATEGORICAL_VALUES_ID, TUMOR_NORMAL_SAMPLES_ID, SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID, SHARED_CONCEPTS_RESTRICTED_ID])
    def "Trial visit dimension elements are returned for all unrestricted studies"() {
        given: "All studies are loaded and I do have unlimited access"

        when: "I try to fetch all trial visits"
        def dimensionName = "trial visit"
        def responseData = get([
                path      : PATH_DIMENSION + "/$dimensionName/elements",
                acceptType: JSON
        ])

        then: "the list of trial visits for unrestricted studies is returned"
        responseData.elements.size() == 19
    }

    @RequiresStudy([EHR_ID, EHR_HIGHDIM_ID, CLINICAL_TRIAL_ID, CATEGORICAL_VALUES_ID, TUMOR_NORMAL_SAMPLES_ID, SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID, SHARED_CONCEPTS_RESTRICTED_ID])
    def "Trial visit dimension elements are returned for all studies"() {
        given: "All studies are loaded and I do have unlimited access"

        when: "I try to fetch all trial visits"
        def dimensionName = "trial visit"
        def responseData = get([
                path      : PATH_DIMENSION + "/$dimensionName/elements",
                acceptType: JSON,
                user      : ADMIN_USERNAME
        ])

        then: "the list of trial visits for all studies is returned"
        responseData.elements.size() == 21
    }

    def "Invalid dimension name"() {
        when: "I try to fetch dimension with invaid name"
        def dimensionName = "invalid name"
        def responseData = get([
                path      : PATH_DIMENSION + "/$dimensionName/elements",
                acceptType: JSON,
                statusCode: 404
        ])

        then: "Exception is thrown"
        assert responseData.httpStatus == 404
        assert responseData.message == "Dimension '$dimensionName' is not valid or you don't have access"
    }
}
