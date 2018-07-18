/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest.v2

import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired

import static org.springframework.http.HttpStatus.*
import static org.transmartproject.rest.MimeTypes.APPLICATION_JSON
import static org.transmartproject.rest.utils.ResponseEntityUtils.toJson

/**
 * Created by piotrzakrzewski on 09/12/2016.
 */
class StorageControllerSpec extends V2ResourceSpec {

    public static final String FILES_COLLECTION_NAME = 'files'
    public static final String STORAGE_SYSTEM_COLLECTION_NAME = 'storageSystems'


    @Autowired
    SessionFactory sessionFactory

    List<Map> getStorageSystems() {
        def response = get "${contextPath}/storage"
        assert response.statusCode == OK
        def systems = toJson(response)[STORAGE_SYSTEM_COLLECTION_NAME] as List<Map>
        assert systems.size() == 2
        systems
    }

    void storageIndexTest() {
        when:
        def response = get "${contextPath}/files"
        then:
        response.statusCode == OK
        def json = toJson(response)
        def filesCollections = json[FILES_COLLECTION_NAME]
        filesCollections.size() == 1
        def fileCollection = filesCollections[0]
        fileCollection.name == '1000 genemes VCFs'
        fileCollection.uuid == 'ys8ib-4zz18-cyw4o6pmrxrixnr'
        fileCollection.study == 'storage_study'
        fileCollection.sourceSystem == storageSystems[0].id
    }

    void postFileLinkTest() {
        when:
        def bodyContent = ['name'        : 'new file Link',
                           'sourceSystem': storageSystems[0].id,
                           'study'       : 'storage_study2',
                           'uuid'        : 'aaaaa-bbbbb-ccccccccccccccc']
        def postResponse = post "${contextPath}/files", APPLICATION_JSON, APPLICATION_JSON, bodyContent
        then:
        postResponse.statusCode == CREATED

        when:
        def indexResponse = get "${contextPath}/files"

        then:
        indexResponse.statusCode == OK
        def json = toJson(indexResponse)
        def filesCollections = json[FILES_COLLECTION_NAME]
        filesCollections.size() == 2
        def secondCollection = filesCollections[1]
        secondCollection.name == 'new file Link'
        secondCollection.uuid == 'aaaaa-bbbbb-ccccccccccccccc'
        secondCollection.study == 'storage_study2'
        secondCollection.sourceSystem == storageSystems[0].id
    }

    def singleGetTest() {
        when:
        def indexResponse = get("${contextPath}/files")

        then:
        def json = toJson(indexResponse)
        int fileId = json[FILES_COLLECTION_NAME][0].id

        when:
        def response = get("${contextPath}/files/$fileId")

        then:
        response.statusCode == OK
        def json2 = toJson(response)
        json2['name'] == '1000 genemes VCFs'
        json2['uuid'] == 'ys8ib-4zz18-cyw4o6pmrxrixnr'
        json2['study'] == 'storage_study'
        json2['sourceSystem'] == storageSystems[0].id
    }

    void indexByStudyTest() {
        when:
        def response = get "${contextPath}/studies/storage_study/files"
        String expectedCollectionName = 'files'
        then:
        response.statusCode == OK
        def json = toJson(response)
        def collections = json[expectedCollectionName]
        collections.size() == 1
        collections[0].name == '1000 genemes VCFs'
        collections[0].uuid == 'ys8ib-4zz18-cyw4o6pmrxrixnr'
        collections[0].study == 'storage_study'
        collections[0].sourceSystem == storageSystems[0].id
    }

    void storageSystemPostTest() {
        def bodyContent = [
                'name'                 : 'mongodb at The Hyve',
                'systemType'           : 'mongodb',
                'url'                  : 'https://mongodb.thehyve.net:5467',
                'systemVersion'        : '3.4',
                'singleFileCollections': true,
        ]

        when:
        def postResponse = post("${contextPath}/storage", APPLICATION_JSON, APPLICATION_JSON, bodyContent)
        then:
        postResponse.statusCode == CREATED
        def json = toJson(postResponse)
        json['id'] != null
        json['id'] instanceof Number
        def storageId = json['id'] as Long

        when:
        def objectResponse = get("${contextPath}/storage/${storageId}")
        then:
        objectResponse.statusCode == OK
        def objectJson = toJson(objectResponse)
        Map<String, Object> idOnlyMap = (objectJson - bodyContent)
        idOnlyMap.size() == 1
        idOnlyMap.id == storageId

        when:
        def indexResponseAfter = get("${contextPath}/storage")
        then:
        def indexResponseAfterJson = toJson(indexResponseAfter)
        def itemsAfter = indexResponseAfterJson[STORAGE_SYSTEM_COLLECTION_NAME] as List
        indexResponseAfter.statusCode == OK
        itemsAfter.size() == 3
        objectJson in itemsAfter
    }

    def linkUpdateTest() {
        def indexResponse = get("${contextPath}/files")
        def indexJson = toJson(indexResponse)
        int fileId = indexJson[FILES_COLLECTION_NAME][0].id
        def bodyContent = ['name'        : 'updated name',
                           'sourceSystem': storageSystems[0].id,
                           'study'       : 'storage_study2',
                           'uuid'        : 'aaaaa-bbbbb-ccccccccccccccc']
        when:
        def before = get "${contextPath}/files/$fileId"
        then:
        before.statusCode == OK
        def beforeJson = toJson(before)

        when:
        def updateResponse = put "${contextPath}/files/$fileId", APPLICATION_JSON, APPLICATION_JSON, bodyContent
        then:
        updateResponse.statusCode == OK

        when:
        def after = get "${contextPath}/files/$fileId"
        then:
        after.statusCode == OK
        def afterJson = toJson(after)
        afterJson['name'] == 'updated name'

        when:
        def putBack = put "${contextPath}/files/$fileId", APPLICATION_JSON, APPLICATION_JSON, beforeJson
        then:
        putBack.statusCode == OK
        def putBackJson = toJson(putBack)
        putBackJson['name'] == beforeJson['name']
    }

    def storageSystemGetTest() {
        def storageId = storageSystems[0].id

        when:
        def response = get("${contextPath}/storage/$storageId")

        then:
        response.statusCode == OK
        def json = toJson(response)
        json['name'] == 'arvados keep at The Hyve'
        json['systemType'] == 'arvados'
        json['systemVersion'] == 'v1'
        json['url'] == 'https://arvbox-pro-dev.thehyve.net'
        json['singleFileCollections'] == false
    }

    def storageSystemDeleteTest() {
        def storageId = storageSystems[1].id

        when:
        def beforeResponse = get("${contextPath}/storage/${storageId}")
        then:
        beforeResponse.statusCode == OK

        when:
        def deleteResponse = delete("${contextPath}/storage/${storageId}")
        then:
        deleteResponse.statusCode == NO_CONTENT

        when:
        def afterResponse = get("${contextPath}/storage/${storageId}")
        then:
        afterResponse.statusCode == NOT_FOUND
    }

}
