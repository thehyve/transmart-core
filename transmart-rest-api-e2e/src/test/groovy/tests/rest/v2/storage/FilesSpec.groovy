package tests.rest.v2.storage

import base.RESTSpec

import static config.Config.*

/**
 *  external file links
 *  TMPREQ-19 Support linking to external data in Arvados from tranSMART API
 */
class FilesSpec extends RESTSpec{


    def storageId

    def setup(){
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def responseDataAll = get([path: PATH_FILES, acceptType: contentTypeForJSON])
        responseDataAll.files.each{
            delete([path: PATH_FILES + "/${it.id}"])
        }

        responseDataAll = get([path: PATH_STORAGE, acceptType: contentTypeForJSON])
        responseDataAll.storageSystems.each{
            delete([path: PATH_STORAGE + "/${it.id}"])
        }

        def sourceSystem = [
                'name':'Arvbox at The Hyve',
                'systemType':'Arvados',
                'url':'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion':'v1',
                'singleFileCollections':false,
        ]
        def responseData = post([path: PATH_STORAGE, body: sourceSystem])
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
        def responseData = post([path: PATH_FILES, body: new_file_link])
        def id = responseData.id

        then:
        assert responseData.id != null
        assert responseData.name == 'new file Link'
        assert responseData.sourceSystem == storageId
        assert responseData.study == 'EHR'
        assert responseData.uuid == 'aaaaa-bbbbb-ccccccccccccccc'

        when:
        responseData = get([path: PATH_FILES + "/${id}", acceptType: contentTypeForJSON])

        then:
        assert responseData.id == id
        assert responseData.name == 'new file Link'
        assert responseData.sourceSystem == storageId
        assert responseData.study == 'EHR'
        assert responseData.uuid == 'aaaaa-bbbbb-ccccccccccccccc'

        when:
        def responseDataAll = get([path: PATH_FILES, acceptType: contentTypeForJSON])

        then:
        assert responseDataAll.files.contains(responseData)

        when:
        new_file_link.name = 'new file Link renamed'
        responseData = put([path: PATH_FILES + "/${id}", body: toJSON(new_file_link)])

        then:
        assert responseData.id == id
        assert responseData.name == 'new file Link renamed'
        assert responseData.sourceSystem == storageId
        assert responseData.study == 'EHR'
        assert responseData.uuid == 'aaaaa-bbbbb-ccccccccccccccc'

        when:
        responseData = delete([path: PATH_FILES + "/${id}"])
        assert responseData == null
        responseData = get([path: PATH_FILES + "/${id}", acceptType: contentTypeForJSON, statusCode: 404])

        then:
        assert responseData.status == 404
        assert responseData.error == 'Not Found'
        assert responseData.message == 'No message available'
        assert responseData.path == "/${PATH_FILES}/${id}"
    }

    /**
     *  given: "There are multiple storage systems with file links"
     *  when: "I get the list of file links"
     *  then: "the list of files has several sourceSystem ids"
     */
    def "post and get from multiple storage systems"() {
        given: "There are multiple storage systems with file links"
        def sourceSystem = [
                'name':'Arvbox at The Hyve 2',
                'systemType':'Arvados',
                'url':'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion':'v1',
                'singleFileCollections':false,
        ]
        def storageId2 = post([path: PATH_STORAGE, body: sourceSystem]).id

        def new_file_link1 = [
                'name'        : 'file in storage 1',
                'sourceSystem': storageId,
                'study'       : 'EHR',
                'uuid'        : 'bbbbb-ccccccccccccccc',
        ]

        def new_file_link2 = [
                'name'        : 'file in storage 2',
                'sourceSystem': storageId2,
                'study'       : 'EHR',
                'uuid'        : 'aaaaa-ccccccccccccccc',
        ]

        def fileID1 = post([path: PATH_FILES, body: new_file_link1]).id

        def fileID2 = post([path: PATH_FILES, body: new_file_link2]).id

        when: "I get the list of file links"
        def responseData = get([path: PATH_FILES, acceptType: contentTypeForJSON])

        then: "the list of files has several sourceSystem ids"
        def files = responseData.files as List
        def sourceSystemIDs = files*.sourceSystem as List
        assert sourceSystemIDs.containsAll(storageId, storageId2)
    }

    /**
     *  post invalid
     */
    def "post invalid values"() {
        given:
        def new_file_link = [
                'name'        : 'new file Link',
                'sourceSystem': storageId,
                'study'       : null,
                'uuid'        : 'aaaaa-bbbbb-ccccccccccccccc',
        ]

        when:
        def responseData = post([path: PATH_FILES, body: new_file_link, statusCode: 422])

        then:
        assert responseData.errors.size() == 1
        assert responseData.errors[0].field == 'study'
        assert responseData.errors[0].message == 'Property [study] of class [class org.transmartproject.db.storage.LinkedFileCollection] cannot be null'
        assert responseData.errors[0].'rejected-value' == null
        assert responseData.errors[0].object == 'org.transmartproject.db.storage.LinkedFileCollection'
    }

    /**
     *  post empty
     */
    def "post empty"() {
        when:
        def responseData = post([path: PATH_FILES, statusCode: 422])

        then:
        assert responseData.errors.size() == 4
        responseData.errors.each {['sourceSystem', 'study', 'name', 'uuid'].contains(it.field)}
    }

    /**
     *  get nonexistent
     */
    def "get nonexistent"() {
        when:
        def responseData = get([path: PATH_FILES + "/0", acceptType: contentTypeForJSON, statusCode: 404])

        then:
        assert responseData.status == 404
        assert responseData.error == 'Not Found'
        assert responseData.message == 'No message available'
        assert responseData.path == "/${PATH_FILES}/0"
    }

    /**
     *  put invalid
     */
    def "put invalid"() {
        given:
        def new_file_link = [
                'name'        : 'new file Link',
                'sourceSystem': storageId,
                'study'       : 'EHR',
                'uuid'        : 'aaaaa-bbbbb-ccccccccccccccc',
        ]
        def responseData = post([path: PATH_FILES, body: toJSON(new_file_link)])
        def id = responseData.id
        new_file_link.uuid = null

        when:
        responseData = put([
                path: (PATH_FILES +"/${id}"),
                body: toJSON(new_file_link),
                statusCode: 422])

        then:
        assert responseData.errors.size() == 1
        assert responseData.errors[0].field == 'uuid'
        assert responseData.errors[0].message == 'Property [uuid] of class [class org.transmartproject.db.storage.LinkedFileCollection] cannot be null'
        assert responseData.errors[0].'rejected-value' == null
        assert responseData.errors[0].object == 'org.transmartproject.db.storage.LinkedFileCollection'
    }

    /**
     *  put nonexistent
     */
    def "put nonexistent"() {
        given:
        def new_file_link = [
                'name'        : 'new file Link',
                'sourceSystem': storageId,
                'study'       : 'EHR',
                'uuid'        : 'aaaaa-bbbbb-ccccccccccccccc',
        ]

        when:
        def responseData = put([path: PATH_FILES +"/0", body: toJSON(new_file_link), statusCode: 404])

        then:
        assert responseData.status == 404
        assert responseData.error == 'Not Found'
        assert responseData.message == 'No message available'
        assert responseData.path == "/${PATH_FILES}/0"
    }

    /**
     *  no access
     */
    def "no access"(){
        given:
        setUser(DEFAULT_USERNAME, DEFAULT_PASSWORD)
        def new_file_link = [
                'name'        : 'new file Link',
                'sourceSystem': storageId,
                'study'       : 'EHR',
                'uuid'        : 'aaaaa-bbbbb-ccccccccccccccc',
        ]

        when:
        def responseData = post([path: PATH_FILES, body: toJSON(new_file_link), statusCode: 403])

        then:
        assert responseData.httpStatus == 403
        assert responseData.message == 'Creating new Linked File Collections is an admin action'
        assert responseData.type == 'AccessDeniedException'

        when:
        new_file_link.name = 'new file Link renamed'
        responseData = put([path: PATH_FILES + "/0", body: toJSON(new_file_link), statusCode: 403])

        then:
        assert responseData.httpStatus == 403
        assert responseData.message == 'updating a linked file entry is an admin action'
        assert responseData.type == 'AccessDeniedException'

        when:
        responseData = delete([path: PATH_FILES + "/0", statusCode: 403])

        then:
        assert responseData.httpStatus == 403
        assert responseData.message == 'Removing a linked file entry is an admin action'
        assert responseData.type == 'AccessDeniedException'
    }
}
