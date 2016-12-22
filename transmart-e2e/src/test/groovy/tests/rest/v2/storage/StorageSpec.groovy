package tests.rest.v2.storage

import base.RESTSpec

import static config.Config.*

class StorageSpec extends RESTSpec{

    def setup() {
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def responseDataAll = get(PATH_FILES)
        responseDataAll.files.each{
            delete(PATH_FILES + "/${it.id}")
        }

        responseDataAll = get(PATH_STORAGE)
        responseDataAll.storageSystems.each{
            delete(PATH_STORAGE + "/${it.id}")
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
        def responseData = post(PATH_STORAGE, toJSON(sourceSystem))
        def id = responseData.id

        then:
        assert responseData.id != null
        assert responseData.name == sourceSystem.name
        assert responseData.singleFileCollections == sourceSystem.singleFileCollections
        assert responseData.systemType == sourceSystem.systemType
        assert responseData.systemVersion == sourceSystem.systemVersion
        assert responseData.url == sourceSystem.url

        when:
        responseData = get(PATH_STORAGE + "/${id}")

        then:
        assert responseData.id == id
        assert responseData.name == sourceSystem.name
        assert responseData.singleFileCollections == sourceSystem.singleFileCollections
        assert responseData.systemType == sourceSystem.systemType
        assert responseData.systemVersion == sourceSystem.systemVersion
        assert responseData.url == sourceSystem.url

        when:
        def responseDataAll = get(PATH_STORAGE)

        then:
        assert responseDataAll.storageSystems.contains(responseData)

        when:
        sourceSystem.name = 'Arvbox at The Hyve renamed'
        responseData = put(PATH_STORAGE + "/${id}", toJSON(sourceSystem))

        then:
        assert responseData.id == id
        assert responseData.name == sourceSystem.name
        assert responseData.singleFileCollections == sourceSystem.singleFileCollections
        assert responseData.systemType == sourceSystem.systemType
        assert responseData.systemVersion == sourceSystem.systemVersion
        assert responseData.url == sourceSystem.url

        when:
        responseData = delete(PATH_STORAGE + "/${id}")
        assert responseData == null
        responseData = get(PATH_STORAGE + "/${id}")

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
        def responseData = post(PATH_STORAGE, toJSON(sourceSystem))

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
        def responseData = post(PATH_STORAGE, sourceSystem)

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
        def responseData = post(PATH_STORAGE, toJSON(sourceSystem))

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
        def responseData = post(PATH_STORAGE, null)

        then:
        assert responseData.errors.size() == 4
    }

    /**
     *  get invalid
     */
    def "get invalid value"(){
        when:
        def responseData = get(PATH_STORAGE + "/some_letters")

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
        def responseData = get(PATH_STORAGE + "/0")

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
        def responseData = post(PATH_STORAGE, toJSON(sourceSystem))
        sourceSystem.singleFileCollections = 'bad_value'
        responseData = post(PATH_STORAGE + "/${responseData.id}", sourceSystem)

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
        def responseData = post(PATH_STORAGE + "/${id}", sourceSystem)

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
        def responseData = post(PATH_STORAGE, toJSON(sourceSystem))

        then:
        assert responseData.httpStatus == 403
        assert responseData.message == 'Creating new storage system entry is an admin action'
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
        def responseData = post(PATH_STORAGE, toJSON(sourceSystem))
        setUser(DEFAULT_USERNAME, DEFAULT_PASSWORD)
        responseData = delete(PATH_STORAGE + "/${responseData.id}")

        then:
        assert responseData.httpStatus == 403
        assert responseData.message == 'Removing a storage system entry is an admin action'
        assert responseData.type == 'AccessDeniedException'
    }



}
