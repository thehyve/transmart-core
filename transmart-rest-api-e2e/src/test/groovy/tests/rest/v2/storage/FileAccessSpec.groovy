package tests.rest.v2.storage

import base.RESTSpec
import base.RestCall

import static config.Config.ADMIN_PASSWORD
import static config.Config.ADMIN_USERNAME
import static config.Config.DEFAULT_PASSWORD
import static config.Config.DEFAULT_USERNAME
import static config.Config.PATH_FILES
import static config.Config.PATH_STORAGE
import static config.Config.PATH_STUDIES
import static config.Config.SHARED_CONCEPTS_RESTRICTED_ID
import static config.Config.UNRESTRICTED_PASSWORD
import static config.Config.UNRESTRICTED_USERNAME

class FileAccessSpec extends RESTSpec {

    def storageId
    def file_link

    def setup(){
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def responseDataAll = get(PATH_FILES)
        responseDataAll.files.each{
            delete(PATH_FILES + "/${it.id}")
        }

        responseDataAll = get(PATH_STORAGE)
        responseDataAll.storageSystems.each{
            delete(PATH_STORAGE + "/${it.id}")
        }

        def sourceSystem = [
                'name':'Arvbox at The Hyve',
                'systemType':'Arvados',
                'url':'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion':'v1',
                'singleFileCollections':false,
        ]
        def responseData = post(PATH_STORAGE, toJSON(sourceSystem))
        storageId = responseData.id

        def new_file_link = [
                'name'        : 'new file Link',
                'sourceSystem': storageId,
                'study'       : SHARED_CONCEPTS_RESTRICTED_ID,
                'uuid'        : 'aaaaa-bbbbb-ccccccccccccccc',
        ]
        file_link = post(PATH_FILES, toJSON(new_file_link))
    }

    /**
     *  given: "a file is attached to a restricted study and I do not have access"
     *  when: "I get files for that study"
     *  then: "I get an access error"
     */
    def "get files by study"(){
        given: "a file is attached to a restricted study and I do not have access"
        setUser(DEFAULT_USERNAME, DEFAULT_PASSWORD)
        RestCall testRequest = new RestCall(PATH_STUDIES+"/${SHARED_CONCEPTS_RESTRICTED_ID}/files", contentTypeForJSON);
        testRequest.statusCode = 403

        when: "I get files for that study"
        def responseData = get(testRequest)

        then: "I get an access error"
        assert responseData.httpStatus == 403
        assert responseData.type == 'AccessDeniedException'
        assert responseData.message == "Access denied to study or study does not exist: ${SHARED_CONCEPTS_RESTRICTED_ID}"
    }

    /**
     *  given: "a file is attached to a restricted study and I have access"
     *  when: "I get files for that study"
     *  then: "I get a list of files"
     */
    def "get files by study unrestricted"(){
        given: "a file is attached to a restricted study and I have access"
        setUser(UNRESTRICTED_USERNAME, UNRESTRICTED_PASSWORD)
        RestCall testRequest = new RestCall(PATH_STUDIES+"/${SHARED_CONCEPTS_RESTRICTED_ID}/files", contentTypeForJSON);

        when: "I get files for that study"
        def responseData = get(testRequest)

        then: "I get a list of files"
        assert responseData.files.each {
            assert it == file_link
        }
    }
}
