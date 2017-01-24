package tests.rest.v2.storage

import base.RESTSpec

import static config.Config.*

/**
 *  external storage
 *  TMPREQ-19 Support linking to external data in Arvados from tranSMART API
 */
class StorageSpec extends RESTSpec{

    def setup() {
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def responseDataAll = get([path: PATH_FILES, acceptType: contentTypeForJSON])
        responseDataAll.files.each{
            delete([path: PATH_FILES + "/${it.id}"])
        }

        responseDataAll = get([path: PATH_STORAGE, acceptType: contentTypeForJSON])
        responseDataAll.storageSystems.each{
            delete([path: PATH_STORAGE + "/${it.id}"])
        }
    }

    /**
     *  post, get, put, delete
     */
    def "post, get, put, delete"(){
        given:
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def sourceSystem = [
                'name':'Arvbox at The Hyve',
                'systemType':'Arvados',
                'url':'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion':'v1',
                'singleFileCollections':false,
        ]

        when:
        def responseData = post([path: PATH_STORAGE, body: toJSON(sourceSystem)])
        def id = responseData.id

        then:
        assert responseData.id != null
        assert responseData.name == sourceSystem.name
        assert responseData.singleFileCollections == sourceSystem.singleFileCollections
        assert responseData.systemType == sourceSystem.systemType
        assert responseData.systemVersion == sourceSystem.systemVersion
        assert responseData.url == sourceSystem.url

        when:
        responseData = get([path: PATH_STORAGE + "/${id}", acceptType: contentTypeForJSON])

        then:
        assert responseData.id == id
        assert responseData.name == sourceSystem.name
        assert responseData.singleFileCollections == sourceSystem.singleFileCollections
        assert responseData.systemType == sourceSystem.systemType
        assert responseData.systemVersion == sourceSystem.systemVersion
        assert responseData.url == sourceSystem.url

        when:
        def responseDataAll = get([path: PATH_STORAGE, acceptType: contentTypeForJSON])

        then:
        assert responseDataAll.storageSystems.contains(responseData)

        when:
        sourceSystem.name = 'Arvbox at The Hyve renamed'
        responseData = put([path: PATH_STORAGE + "/${id}", body: toJSON(sourceSystem)])

        then:
        assert responseData.id == id
        assert responseData.name == sourceSystem.name
        assert responseData.singleFileCollections == sourceSystem.singleFileCollections
        assert responseData.systemType == sourceSystem.systemType
        assert responseData.systemVersion == sourceSystem.systemVersion
        assert responseData.url == sourceSystem.url

        when:
        responseData = delete([path: PATH_STORAGE + "/${id}"])
        assert responseData == null
        responseData = get([path: PATH_STORAGE + "/${id}", acceptType: contentTypeForJSON, statusCode: 404])

        then:
        assert responseData.status == 404
        assert responseData.error == 'Not Found'
        assert responseData.message == 'No message available'
        assert responseData.path == "/v2/storage/${id}"
    }

    /**
     *  post invalid
    */
    def"post missing values"(){
        given:
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def sourceSystem = [
                'name':'post invalid',
                'systemType':'Arvados',
                'url':'http://arvbox-pro-dev.thehyve.net/',
        ]

        when:
        def responseData = post([path: PATH_STORAGE, body: toJSON(sourceSystem), statusCode: 422])

        then:
        assert responseData.errors[0].field == 'systemVersion'
        assert responseData.errors[0].message == 'Property [systemVersion] of class [class org.transmartproject.db.storage.StorageSystem] cannot be null'
        assert responseData.errors[0].'rejected-value' == null
    }

    /**
     *  post invalid
     */
    def "post invalid json format"(){
        given:
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def sourceSystem = toJSON([
                'name':'Arvbox at The Hyve',
                'systemType':'Arvados',
                'url':'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion':'v1',
                'singleFileCollections':false,
        ])
        sourceSystem = sourceSystem.take(20)

        when:
        def responseData = post([path: PATH_STORAGE, body: toJSON(sourceSystem), statusCode: 422])

        then:
        assert responseData.errors.size() == 4
    }

    /**
     *  post invalid
     */
    def "post invalid value"(){
        given:
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def sourceSystem = [
                'name':'Arvbox at The Hyve',
                'systemType':'Arvados',
                'url':'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion':'v1',
                'singleFileCollections':'bad_value',
        ]

        when:
        def responseData = post([path: PATH_STORAGE, body: toJSON(sourceSystem), statusCode: 422])

        then:
        assert responseData.errors.size() == 1
        assert responseData.errors[0].field == 'singleFileCollections'
        assert responseData.errors[0].message == 'Property singleFileCollections is type-mismatched'
        assert responseData.errors[0].'rejected-value' == 'bad_value'
    }

    /**
     *  post empty
     */
    def "post invalid empty"(){
        given:
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)

        when:
        def responseData = post([path: PATH_STORAGE, statusCode: 422])

        then:
        assert responseData.errors.size() == 4
    }

    /**
     *  get invalid
     */
    def "get invalid value"(){
        when:
        def responseData = get([path: PATH_STORAGE + "/some_letters", acceptType: contentTypeForJSON, statusCode: 404])

        then:
        assert responseData.status == 404
        assert responseData.error == "Not Found"
        assert responseData.path == "/v2/storage/some_letters"
    }

    /**
     *  get nonexistent
     */
    def "get nonexistent"(){
        when:
        def responseData = get([path: PATH_STORAGE + "/0", acceptType: contentTypeForJSON, statusCode: 404])

        then:
        assert responseData.status == 404
        assert responseData.error == "Not Found"
        assert responseData.path == "/v2/storage/0"
    }

    /**
     *  put invalid
     */
    def "put invalid values"(){
        given:
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def sourceSystem = [
                'name':'Arvbox at The Hyve',
                'systemType':'Arvados',
                'url':'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion':'v1',
                'singleFileCollections':false,
        ]

        when:
        def responseData = post([path: PATH_STORAGE, body: toJSON(sourceSystem)])
        sourceSystem.singleFileCollections = 'bad_value'
        responseData = put([path: PATH_STORAGE + "/${responseData.id}", body: toJSON(sourceSystem), statusCode: 422])

        then:
        assert responseData.errors.size() == 1
        assert responseData.errors[0].field == 'singleFileCollections'
        assert responseData.errors[0].message == 'Property singleFileCollections is type-mismatched'
        assert responseData.errors[0].'rejected-value' == 'bad_value'
    }

    /**
     *  put nonexistent
     */
    def "put nonexistent"(){
        given:
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def id = 0
        def sourceSystem = [
                'name':'Arvbox at The Hyve',
                'systemType':'Arvados',
                'url':'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion':'v1',
                'singleFileCollections':false,
        ]

        when:
        def responseData = put([path: PATH_STORAGE + "/${id}", body: toJSON(sourceSystem), statusCode: 404])

        then:
        assert responseData.status == 404
        assert responseData.error == "Not Found"
        assert responseData.path == "/v2/storage/${id}"
    }

    /**
     *  no access
     */
    def "post no access"(){
        given:
        setUser(DEFAULT_USERNAME, DEFAULT_PASSWORD)
        def sourceSystem = [
                'name':'Arvbox at The Hyve',
                'systemType':'Arvados',
                'url':'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion':'v1',
                'singleFileCollections':false,
        ]

        when:
        def responseData = post([path: PATH_STORAGE, body: toJSON(sourceSystem), statusCode: 403])

        then:
        assert responseData.httpStatus == 403
        assert responseData.type == 'AccessDeniedException'
    }

    /**
     *  no access
     */
    def "delete no access"(){
        given:
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def sourceSystem = [
                'name':'Arvbox at The Hyve',
                'systemType':'Arvados',
                'url':'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion':'v1',
                'singleFileCollections':false,
        ]

        when:
        def responseData = post([path: PATH_STORAGE, body: toJSON(sourceSystem)])
        setUser(DEFAULT_USERNAME, DEFAULT_PASSWORD)
        responseData = delete([ path: PATH_STORAGE + "/${responseData.id}", statusCode: 403])

        then:
        assert responseData.httpStatus == 403
        assert responseData.message == 'Removing a storage system entry is an admin action'
        assert responseData.type == 'AccessDeniedException'
    }



}
