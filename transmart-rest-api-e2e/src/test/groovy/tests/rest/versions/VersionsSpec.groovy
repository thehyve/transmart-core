/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.versions

import base.RESTSpec

import static base.ContentTypeFor.contentTypeForJSON
import static config.Config.NON_EXISTING_API_VERSION
import static config.Config.VERSIONS_PATH

class VersionsSpec extends RESTSpec {

    /**
     *  When: "I fetch supported versions"
     *  Then: "I get both v1 and v2 as response"
     */
    def "fetch versions"() {
        when: "I fetch supported versions"
        def responseData = get([path: VERSIONS_PATH, acceptType: contentTypeForJSON])

        then: "I get both v1 and v2 as response"
        assert responseData.versions.size() == 2
    }

    /**
     *  When: "I fetch version v1"
     *  Then: "I get both v1 response"
     */
    def "fetch version v1"() {
        when: "I fetch version v1"
        def responseData = get([path: "${VERSIONS_PATH}/v1", acceptType: contentTypeForJSON])

        then: "I get both v1 response"
        assert responseData.id == 'v1'
        assert responseData.prefix == '/v1'
    }

    /**
     *  When: "I fetch version v0"
     *  Then: "I get a 404 response"
     */
    def "fetch non existing version"() {
        when: "I fetch version v0"
        def responseData = get([
                path      : "${VERSIONS_PATH}/${NON_EXISTING_API_VERSION}",
                acceptType: contentTypeForJSON,
                statusCode: 404
        ])

        then: "I get a 404 response"
        assert responseData.httpStatus == 404
        assert responseData.type == 'NoSuchResourceException'
        assert responseData.message == "Version not available: ${NON_EXISTING_API_VERSION}."
    }

}
