package tests.rest.v2.storage

import base.RESTSpec

import static config.Config.ADMIN_PASSWORD
import static config.Config.ADMIN_USERNAME
import static config.Config.PATH_FILES
import static config.Config.PATH_STORAGE

class FilesSpec extends RESTSpec{


    def storageId

    def setup(){
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def sourceSystem = [
                'name':'Arvbox at The Hyve',
                'systemType':'Arvados',
                'url':'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion':'v1',
                'singleFileCollections':false,
        ]
        def responseData = post(PATH_STORAGE, toJSON(sourceSystem))
        storageId = responseData.id
    }

    /**
     *  post, get, put, delete
     */
    def "post, get, put, delete"(){
        given:
        def new_file_link = [
                'name'        : 'new file Link',
                'sourceSystem': storageId,
                'study'       : 'EHR',
                'uuid'        : 'aaaaa-bbbbb-ccccccccccccccc',
        ]

        when:
        def responseData = post(PATH_FILES, toJSON(new_file_link))
        def id = responseData.id

        then:
        assert responseData.id != null
        assert responseData.name == 'new file Link'
        assert responseData.sourceSystem == storageId
        assert responseData.study == 'EHR'
        assert responseData.uuid == 'aaaaa-bbbbb-ccccccccccccccc'

        when:
        responseData = get(PATH_FILES + "/${id}")

        then:
        assert responseData.id == id
        assert responseData.name == 'new file Link'
        assert responseData.sourceSystem == storageId
        assert responseData.study == 'EHR'
        assert responseData.uuid == 'aaaaa-bbbbb-ccccccccccccccc'

        when:
        def responseDataAll = get(PATH_FILES)

        then:
        assert responseDataAll.files.contains(responseData)

        when:
        new_file_link.name = 'new file Link renamed'
        responseData = put(PATH_FILES + "/${id}", toJSON(new_file_link))

        then:
        assert responseData.id == id
        assert responseData.name == 'new file Link renamed'
        assert responseData.sourceSystem == storageId
        assert responseData.study == 'EHR'
        assert responseData.uuid == 'aaaaa-bbbbb-ccccccccccccccc'

        when:
        responseData = delete(PATH_FILES + "/${id}")
        assert responseData == null
        responseData = get(PATH_FILES + "/${id}")

        then:
        assert responseData.status == 404
        assert responseData.error == 'Not Found'
        assert responseData.message == 'No message available'
        assert responseData.path == "/v2/files/${id}"
    }


    /**
     *  post invalid
     */

    /**
     *  post empty
     */

    /**
     *  get invalid
     */

    /**
     *  get nonexistent
     */

    /**
     *  put invalid
     */

    /**
     *  put nonexistent
     */

    /**
     *  get all
     */

    /**
     *  get single
     */


}
