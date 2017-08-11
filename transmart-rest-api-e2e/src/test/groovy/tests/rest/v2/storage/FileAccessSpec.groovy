/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2.storage

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static config.Config.*

@RequiresStudy(SHARED_CONCEPTS_RESTRICTED_ID)
class FileAccessSpec extends RESTSpec {

    def storageId
    def file_link

    def setup() {
        def responseDataAll = get([path: PATH_FILES, acceptType: JSON, user: ADMIN_USERNAME])
        responseDataAll.files.each {
            delete([path: PATH_FILES + "/${it.id}", statusCode: 204, user: ADMIN_USERNAME])
        }

        responseDataAll = get([path: PATH_STORAGE, acceptType: JSON, user: ADMIN_USERNAME])
        responseDataAll.storageSystems.each {
            delete([path: PATH_STORAGE + "/${it.id}", statusCode: 204, user: ADMIN_USERNAME])
        }

        def sourceSystem = [
                'name'                 : 'Arvbox at The Hyve',
                'systemType'           : 'Arvados',
                'url'                  : 'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion'        : 'v1',
                'singleFileCollections': false,
        ]
        def responseData = post([path: PATH_STORAGE, body: toJSON(sourceSystem), statusCode: 201, user: ADMIN_USERNAME])
        storageId = responseData.id

        def new_file_link = [
                'name'        : 'new file Link',
                'sourceSystem': storageId,
                'study'       : SHARED_CONCEPTS_RESTRICTED_ID,
                'uuid'        : 'aaaaa-bbbbb-ccccccccccccccc',
        ]
        file_link = post([path: PATH_FILES, body: toJSON(new_file_link), statusCode: 201, user: ADMIN_USERNAME])
    }

    /**
     *  given: "a file is attached to a restricted study and I do not have access"
     *  when: "I get files for that study"
     *  then: "I get an access error"
     */
    def "get files by study"() {
        given: "a file is attached to a restricted study and I do not have access"

        when: "I get files for that study"
        def responseData = get([
                path      : PATH_STUDIES + "/${SHARED_CONCEPTS_RESTRICTED_ID}/files",
                acceptType: JSON,
                statusCode: 403
        ])

        then: "I get an access error"
        assert responseData.httpStatus == 403
        assert responseData.type == 'AccessDeniedException'
    }

    /**
     *  given: "a file is attached to a restricted study and I have access"
     *  when: "I get files for that study"
     *  then: "I get a list of files"
     */
    def "get files by study unrestricted"() {
        given: "a file is attached to a restricted study and I have access"

        when: "I get files for that study"
        def responseData = get([
                path      : PATH_STUDIES + "/${SHARED_CONCEPTS_RESTRICTED_ID}/files",
                acceptType: JSON,
                user      : ADMIN_USERNAME
        ])

        then: "I get a list of files"
        assert responseData.files.each {
            assert it == file_link
        }
    }
}
