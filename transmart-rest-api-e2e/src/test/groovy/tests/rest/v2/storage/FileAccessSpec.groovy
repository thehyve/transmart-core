/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2.storage

import annotations.RequiresStudy
import base.RESTSpec
import base.RestHelper
import representations.ErrorResponse

import static base.ContentTypeFor.JSON
import static config.Config.*

@RequiresStudy(SHARED_CONCEPTS_RESTRICTED_ID)
class FileAccessSpec extends RESTSpec {

    def storageId
    def file_link

    def setup() {
        def responseDataAll = get([path: PATH_FILES, acceptType: JSON, user: ADMIN_USER])
        responseDataAll.files.each {
            delete([path: PATH_FILES + "/${it.id}", statusCode: 204, user: ADMIN_USER])
        }

        responseDataAll = get([path: PATH_STORAGE, acceptType: JSON, user: ADMIN_USER])
        responseDataAll.storageSystems.each {
            delete([path: PATH_STORAGE + "/${it.id}", statusCode: 204, user: ADMIN_USER])
        }

        def sourceSystem = [
                'name'                 : 'Arvbox at The Hyve',
                'systemType'           : 'Arvados',
                'url'                  : 'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion'        : 'v1',
                'singleFileCollections': false,
        ]
        def responseData = post([path: PATH_STORAGE, body: sourceSystem, statusCode: 201, user: ADMIN_USER])
        storageId = responseData.id

        def new_file_link = [
                'name'        : 'new file Link',
                'sourceSystem': storageId,
                'study'       : SHARED_CONCEPTS_RESTRICTED_ID,
                'uuid'        : 'aaaaa-bbbbb-ccccccccccccccc',
        ]
        file_link = post([path: PATH_FILES, body: new_file_link, statusCode: 201, user: ADMIN_USER])
    }

    /**
     *  given: "a file is attached to a restricted study and I do not have access"
     *  when: "I get files for that study"
     *  then: "I get an access error"
     */
    def "get files by study"() {
        given: "a file is attached to a restricted study and I do not have access"

        when: "I get files for that study"
        def responseData = RestHelper.toObject get([
                path      : PATH_STUDIES + "/${SHARED_CONCEPTS_RESTRICTED_ID}/files",
                acceptType: JSON,
                statusCode: 403
        ]), ErrorResponse

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
                user      : ADMIN_USER
        ])

        then: "I get a list of files"
        assert responseData.files.each {
            assert it == file_link
        }
    }
}
