package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec
import groovy.util.logging.Slf4j

import static base.ContentTypeFor.JSON
import static config.Config.*

@Slf4j
class DimensionSpec extends RESTSpec {

    @RequiresStudy([EHR_ID, EHR_HIGHDIM_ID, CLINICAL_TRIAL_ID, CATEGORICAL_VALUES_ID, TUMOR_NORMAL_SAMPLES_ID, SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID, SHARED_CONCEPTS_RESTRICTED_ID])
    def "Dimensions metadata are returned"() {
        given: "All studies are loaded and I do have unlimited access"

        when: "I try to fetch all dimensions"
        def responseData = get([
                path      : PATH_DIMENSION,
                acceptType: JSON
        ])

        then: "the list of dimensions is returned"
        def dimensionNames = responseData.dimensions.name
        dimensionNames as Set ==
                [
                        'study', 'patient', 'concept', 'trial visit',
                        'biomarker', 'assay', 'projection',
                        'end time', 'visit', 'start time', 'missing_value', 'sample_type',
                        'Biomaterial ID', 'Diagnosis ID', 'Biosource ID', 'Images Id',
                        'Position'
                ] as Set
    }

    @RequiresStudy([EHR_ID, EHR_HIGHDIM_ID, CLINICAL_TRIAL_ID, CATEGORICAL_VALUES_ID, TUMOR_NORMAL_SAMPLES_ID, SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID, SHARED_CONCEPTS_RESTRICTED_ID])
    def "Specified dimension metadata is returned"() {
        given: "All studies are loaded and I do have unlimited access"

        when: "I try to fetch the patient dimension"
        def dimensionName = "patient"
        def responseData = get([
                path      : PATH_DIMENSION + "/$dimensionName",
                acceptType: JSON
        ])

        then: "the dimension metadata returned"
        responseData
        responseData.name == dimensionName
        'id' in responseData.fields.name
        'subjectIds' in responseData.fields.name
    }

    @RequiresStudy([EHR_ID, EHR_HIGHDIM_ID, CLINICAL_TRIAL_ID, CATEGORICAL_VALUES_ID, TUMOR_NORMAL_SAMPLES_ID, SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID, SHARED_CONCEPTS_RESTRICTED_ID])
    def "Trial visit dimension elements are returned for all unrestricted studies"() {
        given: "All studies are loaded and I do have unlimited access"

        when: "I try to fetch all trial visits"
        def dimensionName = "trial visit"
        def responseData = post([
                path      : PATH_DIMENSION + "/$dimensionName/elements",
                acceptType: JSON
        ])

        then: "the list of trial visits for unrestricted studies is returned"
        responseData.elements.size() == 28
    }

    @RequiresStudy([EHR_ID, EHR_HIGHDIM_ID, CLINICAL_TRIAL_ID, CATEGORICAL_VALUES_ID, TUMOR_NORMAL_SAMPLES_ID, SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID, SHARED_CONCEPTS_RESTRICTED_ID])
    def "Trial visit dimension elements are returned for all studies"() {
        given: "All studies are loaded and I do have unlimited access"

        when: "I try to fetch all trial visits"
        def dimensionName = "trial visit"
        def responseData = post([
                path      : PATH_DIMENSION + "/$dimensionName/elements",
                acceptType: JSON,
                user      : ADMIN_USER
        ])

        then: "the list of trial visits for all studies is returned"
        responseData.elements.size() == 30
    }

    def "Invalid dimension name"() {
        when: "I try to fetch dimension with invalid name"
        def dimensionName = "invalid name"
        def responseData = post([
                path      : PATH_DIMENSION + "/$dimensionName/elements",
                acceptType: JSON,
                statusCode: 404
        ])

        then: "Exception is thrown"
        assert responseData.httpStatus == 404
        assert responseData.message == "Dimension '$dimensionName' is not valid or you don't have access"
    }
}
