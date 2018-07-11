package tests.rest.v1

import annotations.RequiresV1ApiSupport
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static base.ContentTypeFor.XML
import static config.Config.*

/**
 * These tests will be executed only if the `v1` API
 * is not supported (see config.Config.IS_V1_API_SUPPORTED)
 */
@RequiresV1ApiSupport(false)
class V1ApiDisabledSpec extends RESTSpec {

    def "v1 get studies"() {
        given: "several studies are loaded"

        when: "I request all studies"
        def responseData = get([
                path      : V1_PATH_STUDIES,
                acceptType: JSON,
                statusCode: 403
        ])

        then: "I get 403 response status"
        responseData.message == "Access is denied"
        responseData.error == "Forbidden"
    }

    def "v1 get observations"() {
        when: "I request all observations"
        def responseData = get([
                path      : V1_PATH_OBSERVATIONS,
                acceptType: JSON,
                statusCode: 403
        ])

        then: "I get 403 response status"
        responseData.message == "Access is denied"
        responseData.error == "Forbidden"
    }

    def "v1 post patient_sets"() {
        when: "I create a patient set"
        def responseData = post([
                path       : V1_PATH_PATIENT_SETS,
                contentType: XML,
                body       : '<>',
                statusCode : 403,
                user       : ADMIN_USER
        ])

        then: "I get 403 response status"
        responseData.message == "Access is denied"
        responseData.error == "Forbidden"
    }
}
