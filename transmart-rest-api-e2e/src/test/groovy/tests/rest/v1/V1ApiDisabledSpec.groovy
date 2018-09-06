package tests.rest.v1

import annotations.RequiresStudy
import annotations.RequiresV1ApiSupport
import base.RESTSpec
import org.transmartproject.core.multidimquery.ErrorResponse
import base.RestHelper

import static base.ContentTypeFor.JSON
import static base.ContentTypeFor.XML
import static config.Config.*

/**
 * These tests will be executed only if the `v1` API
 * is not supported (see {@link config.Config#IS_V1_API_SUPPORTED})
 */
@RequiresV1ApiSupport(false)
class V1ApiDisabledSpec extends RESTSpec {

    def "v1 get studies"() {
        given: "several studies are loaded"

        when: "I request all studies"
        def responseData = RestHelper.toObject get([
                path      : V1_PATH_STUDIES,
                acceptType: JSON,
                statusCode: 403
        ]), ErrorResponse

        then: "I get 403 response status"
        responseData.message == "Access is denied"
        responseData.error == "Forbidden"
    }

    @RequiresStudy(EHR_ID)
    def "v1 get observations"() {
        when: "I request all observations"
        def responseData = RestHelper.toObject get([
                path      : V1_PATH_OBSERVATIONS,
                query     : [
                        patients     : [-62, -52, -42],
                        concept_paths: ["\\Public Studies\\EHR\\Demography\\Age\\"]
                ],
                acceptType: JSON,
                statusCode: 403
        ]), ErrorResponse

        then: "I get 403 response status"
        responseData.message == "Access is denied"
        responseData.error == "Forbidden"
    }

    def "v1 post patient_sets"() {
        when: "I create a patient set"
        def body = '<ns4:query_definition xmlns:ns4="http://www.i2b2.org/xsd/cell/crc/psm/1.1/"><specificity_scale>0</specificity_scale><panel><panel_number></panel_number><invert>0</invert><total_item_occurrences>1</total_item_occurrences><item><item_name>Heart Rate</item_name><item_key>\\\\Vital Signs\\Vital Signs\\Heart Rate\\</item_key><tooltip>\\Vital Signs\\Heart Rate\\</tooltip><hlevel>1</hlevel><class>ENC</class></item><panel_timing>ANY</panel_timing></panel><panel><panel_number>2</panel_number><invert>0</invert><total_item_occurrences>1</total_item_occurrences><item><item_name>Age</item_name><item_key>\\\\Public Studies\\Public Studies\\SHARED_CONCEPTS_STUDY_A\\Demography\\Age\\</item_key><tooltip>\\Public Studies\\SHARED_CONCEPTS_STUDY_A\\Demography\\Age\\</tooltip><hlevel>3</hlevel><class>ENC</class><constrain_by_value><value_operator>LT</value_operator><value_constraint>70</value_constraint><value_unit_of_measure>ratio</value_unit_of_measure><value_type>NUMBER</value_type></constrain_by_value></item><panel_timing>ANY</panel_timing></panel><query_timing>ANY</query_timing></ns4:query_definition>'
        def responseData = RestHelper.toObject post([
                path       : V1_PATH_PATIENT_SETS,
                contentType: XML,
                body       : body,
                statusCode : 403,
                user       : ADMIN_USER
        ]), ErrorResponse

        then: "I get 403 response status"
        responseData.message == "Access is denied"
        responseData.error == "Forbidden"
    }
}
