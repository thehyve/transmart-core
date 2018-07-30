/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2.storage

import base.RESTSpec
import base.RestHelper
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonBuilder
import representations.ErrorResponse

import static base.ContentTypeFor.JSON
import static config.Config.*

/**
 *  external storage
 *  TMPREQ-19 Support linking to external data in Arvados from tranSMART API
 */
class StorageSpec extends RESTSpec {

    def setup() {
        def responseDataAll = get([path: PATH_FILES, acceptType: JSON, user: ADMIN_USER])
        responseDataAll.files.each {
            delete([path: PATH_FILES + "/${it.id}", statusCode: 204, user: ADMIN_USER])
        }

        responseDataAll = get([path: PATH_STORAGE, acceptType: JSON, user: ADMIN_USER])
        responseDataAll.storageSystems.each {
            delete([path: PATH_STORAGE + "/${it.id}", statusCode: 204, user: ADMIN_USER])
        }
    }

    /**
     *  post, get, put, delete
     */
    def "post, get, put, delete"() {
        given:
        def sourceSystem = [
                'name'                 : 'Arvbox at The Hyve',
                'systemType'           : 'Arvados',
                'url'                  : 'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion'        : 'v1',
                'singleFileCollections': false,
        ]

        when:
        def responseData = post([path: PATH_STORAGE, body: sourceSystem, statusCode: 201, user: ADMIN_USER])
        def id = responseData.id

        then:
        assert responseData.id != null
        assert responseData.name == sourceSystem.name
        assert responseData.singleFileCollections == sourceSystem.singleFileCollections
        assert responseData.systemType == sourceSystem.systemType
        assert responseData.systemVersion == sourceSystem.systemVersion
        assert responseData.url == sourceSystem.url

        when:
        responseData = get([path: PATH_STORAGE + "/${id}", acceptType: JSON, user: ADMIN_USER])

        then:
        assert responseData.id == id
        assert responseData.name == sourceSystem.name
        assert responseData.singleFileCollections == sourceSystem.singleFileCollections
        assert responseData.systemType == sourceSystem.systemType
        assert responseData.systemVersion == sourceSystem.systemVersion
        assert responseData.url == sourceSystem.url

        when:
        def responseDataAll = get([path: PATH_STORAGE, acceptType: JSON, user: ADMIN_USER])

        then:
        assert responseDataAll.storageSystems.contains(responseData)

        when:
        sourceSystem.name = 'Arvbox at The Hyve renamed'
        responseData = put([path: PATH_STORAGE + "/${id}", body: sourceSystem, user: ADMIN_USER])

        then:
        assert responseData.id == id
        assert responseData.name == sourceSystem.name
        assert responseData.singleFileCollections == sourceSystem.singleFileCollections
        assert responseData.systemType == sourceSystem.systemType
        assert responseData.systemVersion == sourceSystem.systemVersion
        assert responseData.url == sourceSystem.url

        when:
        responseData = delete([path: PATH_STORAGE + "/${id}", statusCode: 204, user: ADMIN_USER])
        assert responseData == null
        responseData = RestHelper.toObject get([path: PATH_STORAGE + "/${id}", acceptType: JSON, statusCode: 404, user: ADMIN_USER]), ErrorResponse

        then:
        assert responseData.status == 404
        assert responseData.error == 'Not Found'
        assert responseData.message == 'No message available'
        assert responseData.path == "/v2/storage/${id}"
    }

    /**
     *  post invalid
     */
    def "post missing values"() {
        given:
        def sourceSystem = [
                'name'      : 'post invalid',
                'systemType': 'Arvados',
                'url'       : 'http://arvbox-pro-dev.thehyve.net/',

        ]

        when:
        def responseData = post([path: PATH_STORAGE, body: sourceSystem, statusCode: 422, user: ADMIN_USER])

        then:
        assert responseData.errors[0].field == 'systemVersion'
        assert responseData.errors[0].message == 'Property [systemVersion] of class [class org.transmartproject.db.storage.StorageSystem] cannot be null'
        assert responseData.errors[0].'rejected-value' == null
    }

    /**
     *  post invalid
     */
    def "post invalid json format"() {
        given:
        def mapper = new ObjectMapper()
        def sourceSystem = mapper.writeValueAsString([
                'name'                 : 'Arvbox at The Hyve',
                'systemType'           : 'Arvados',
                'url'                  : 'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion'        : 'v1',
                'singleFileCollections': false,
        ])
        sourceSystem = sourceSystem.take(20)

        when:
        def responseData = post([path: PATH_STORAGE, body: sourceSystem, statusCode: 422, user: ADMIN_USER])

        then:
        assert responseData.errors.size() == 4
    }

    /**
     *  post invalid
     */
    def "post invalid value"() {
        given:
        def sourceSystem = [
                'name'                 : 'Arvbox at The Hyve',
                'systemType'           : 'Arvados',
                'url'                  : 'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion'        : 'v1',
                'singleFileCollections': 'bad_value',
        ]

        when:
        def responseData = post([path: PATH_STORAGE, body: sourceSystem, statusCode: 422, user: ADMIN_USER])

        then:
        assert responseData.errors.size() == 1
        assert responseData.errors[0].field == 'singleFileCollections'
        assert responseData.errors[0].'rejected-value' == 'bad_value'
    }

    /**
     *  post empty
     */
    def "post invalid empty"() {
        when:
        def responseData = post([path: PATH_STORAGE, statusCode: 422, user: ADMIN_USER])

        then:
        assert responseData.errors.size() == 4
    }

    /**
     *  get invalid
     */
    def "get invalid value"() {
        when:
        def responseData = get([path: PATH_STORAGE + "/some_letters", acceptType: JSON, statusCode: 404])

        then:
        assert responseData.status == 404
        assert responseData.error == "Not Found"
        assert responseData.path == "/v2/storage/some_letters"
    }

    /**
     *  get nonexistent
     */
    def "get nonexistent"() {
        when:
        def responseData = RestHelper.toObject get([path: PATH_STORAGE + "/0", acceptType: JSON, statusCode: 404]), ErrorResponse

        then:
        assert responseData.status == 404
        assert responseData.error == "Not Found"
        assert responseData.path == "/v2/storage/0"
    }

    /**
     *  put invalid
     */
    def "put invalid values"() {
        given:
        def sourceSystem = [
                'name'                 : 'Arvbox at The Hyve',
                'systemType'           : 'Arvados',
                'url'                  : 'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion'        : 'v1',
                'singleFileCollections': false,
        ]

        when:
        def responseData = post([path: PATH_STORAGE, body: sourceSystem, statusCode: 201, user: ADMIN_USER])
        sourceSystem.singleFileCollections = 'bad_value'
        responseData = put([path: PATH_STORAGE + "/${responseData.id}", body: sourceSystem, statusCode: 422, user: ADMIN_USER])

        then:
        assert responseData.errors.size() == 1
        assert responseData.errors[0].field == 'singleFileCollections'
        assert responseData.errors[0].'rejected-value' == 'bad_value'
    }

    /**
     *  put nonexistent
     */
    def "put nonexistent"() {
        given:
        def id = 0
        def sourceSystem = [
                'name'                 : 'Arvbox at The Hyve',
                'systemType'           : 'Arvados',
                'url'                  : 'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion'        : 'v1',
                'singleFileCollections': false,
        ]

        when:
        def responseData = RestHelper.toObject put([path: PATH_STORAGE + "/${id}", body: sourceSystem, statusCode: 404, user: ADMIN_USER]), ErrorResponse

        then:
        assert responseData.status == 404
        assert responseData.error == "Not Found"
        assert responseData.path == "/v2/storage/${id}"
    }

    /**
     *  no access
     */
    def "post no access"() {
        given:
        def sourceSystem = [
                'name'                 : 'Arvbox at The Hyve',
                'systemType'           : 'Arvados',
                'url'                  : 'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion'        : 'v1',
                'singleFileCollections': false,
        ]

        when:
        def responseData = RestHelper.toObject post([path: PATH_STORAGE, body: sourceSystem, statusCode: 403]), ErrorResponse

        then:
        assert responseData.httpStatus == 403
        assert responseData.type == 'AccessDeniedException'
    }

    /**
     *  no access
     */
    def "delete no access"() {
        given:
        def sourceSystem = [
                'name'                 : 'Arvbox at The Hyve',
                'systemType'           : 'Arvados',
                'url'                  : 'http://arvbox-pro-dev.thehyve.net/',
                'systemVersion'        : 'v1',
                'singleFileCollections': false,
        ]

        when:
        def responseData = post([path: PATH_STORAGE, body: sourceSystem, statusCode: 201, user: ADMIN_USER])
        responseData = RestHelper.toObject delete([path: PATH_STORAGE + "/${responseData.id}", statusCode: 403]), ErrorResponse

        then:
        assert responseData.httpStatus == 403
        assert responseData.message == 'Removing a storage system entry is an admin action'
        assert responseData.type == 'AccessDeniedException'
    }
}
