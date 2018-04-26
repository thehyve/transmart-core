/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import grails.converters.JSON
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.storage.StorageSystem
import spock.lang.Ignore

/**
 * Created by piotrzakrzewski on 09/12/2016.
 */
class StorageControllerSpec extends ResourceSpec {

    public static final String VERSION = "v2"
    public static final String FILES_COLLECTION_NAME = 'files'
    public static final String STORAGE_SYSTEM_COLLECTION_NAME = 'storageSystems'


    @Autowired
    SessionFactory sessionFactory

    StorageSystem testStorageSystem() {
        BootStrap.testData.storageTestData.storageSystemList[0]
    }

    StorageSystem testStorageSystem2() {
        BootStrap.testData.storageTestData.storageSystemList[1]
    }

    void storageIndexTest() {
        when:
        def response = get "/$VERSION/files"
        then:
        response.status == 200
        response.json[FILES_COLLECTION_NAME]
        response.json[FILES_COLLECTION_NAME].size() == 1
        response.json[FILES_COLLECTION_NAME][0].name == '1000 genemes VCFs'
        response.json[FILES_COLLECTION_NAME][0].uuid == 'ys8ib-4zz18-cyw4o6pmrxrixnr'
        response.json[FILES_COLLECTION_NAME][0].study == 'storage_study'
        response.json[FILES_COLLECTION_NAME][0].sourceSystem == testStorageSystem().id
    }

    /**
     * FIXME: This test fails randomly.
     * It seems as if first adding a file and then fetching it does not work reliably.
     * There is probably some caching layer that is not properly updated.
     */
    @Ignore
    void postFileLinkTest() {
        when:
        def bodyContent = ['name'        : 'new file Link',
                           'sourceSystem': testStorageSystem().id,
                           'study'       : 'storage_study2',
                           'uuid'        : 'aaaaa-bbbbb-ccccccccccccccc'] as JSON
        def postResponse = post "/$VERSION/files", {
            contentType "application/json"
            json bodyContent
        }
        def indexResponse = get "/$VERSION/files"
        then:
        postResponse.status == 201
        indexResponse.status == 200
        indexResponse.json[FILES_COLLECTION_NAME].size() == 2
        indexResponse.json[FILES_COLLECTION_NAME][1].name == 'new file Link'
        indexResponse.json[FILES_COLLECTION_NAME][1].uuid == 'aaaaa-bbbbb-ccccccccccccccc'
        indexResponse.json[FILES_COLLECTION_NAME][1].study == 'storage_study2'
        indexResponse.json[FILES_COLLECTION_NAME][1].sourceSystem == testStorageSystem().id
    }

    def singleGetTest() {
        when:
        def indexResponse = get("/$VERSION/files")
        int fileId = indexResponse.json[FILES_COLLECTION_NAME][0].id
        def response = get("/$VERSION/files/$fileId")
        then:
        response.status == 200
        response.json['name'] == '1000 genemes VCFs'
        response.json['uuid'] == 'ys8ib-4zz18-cyw4o6pmrxrixnr'
        response.json['study'] == 'storage_study'
        response.json['sourceSystem'] == testStorageSystem().id
    }

    void indexByStudyTest() {
        when:
        def response = get "/$VERSION/studies/storage_study/files"
        String expectedCollectionName = 'files'
        then:
        response.status == 200
        response.json[expectedCollectionName]
        response.json[expectedCollectionName].size() == 1
        response.json[expectedCollectionName][0].name == '1000 genemes VCFs'
        response.json[expectedCollectionName][0].uuid == 'ys8ib-4zz18-cyw4o6pmrxrixnr'
        response.json[expectedCollectionName][0].study == 'storage_study'
        response.json[expectedCollectionName][0].sourceSystem == testStorageSystem().id
    }

    /**
     * FIXME: This test fails randomly.
     * It seems as if first adding a system and then fetching it does not work reliably.
     * There is probably some caching layer that is not properly updated.
     */
    @Ignore
    void storageSystemPostTest() {
        when:
        def indexResponseBefore = get("/$VERSION/storage")
        def bodyContent = [
                'name'                 : 'mongodb at The Hyve',
                'systemType'           : 'mongodb',
                'url'                  : 'https://mongodb.thehyve.net:5467',
                'systemVersion'        : '3.4',
                'singleFileCollections': true,
        ] as JSON
        def postResponse = post "/$VERSION/storage", {
            contentType "application/json"
            json bodyContent
        }
        then:
        postResponse.status == 201
        postResponse.json['id'] != null
        postResponse.json['id'] instanceof Number

        when:
        def storageId = postResponse.json['id'] as Long
        def objectResponse = get("/$VERSION/storage/${storageId}")
        then:
        objectResponse.status == 200
        objectResponse.json['id'] == storageId
        objectResponse.json['name'] == 'mongodb at The Hyve'
        objectResponse.json['systemType'] == 'mongodb'
        objectResponse.json['systemVersion'] == '3.4'
        objectResponse.json['url'] == 'https://mongodb.thehyve.net:5467'
        objectResponse.json['singleFileCollections'] == true

        when:
        def indexResponseAfter = get("/$VERSION/storage")
        def itemsBefore = indexResponseBefore.json[STORAGE_SYSTEM_COLLECTION_NAME] as List
        def itemsAfter = indexResponseAfter.json[STORAGE_SYSTEM_COLLECTION_NAME] as List
        then:
        indexResponseAfter.status == 200
        indexResponseBefore.status == 200
        itemsAfter.size() == (itemsBefore.size() + 1)
        itemsAfter[2]['id'] == storageId
        itemsAfter[2]['name'] == 'mongodb at The Hyve'
        itemsAfter[2]['systemType'] == 'mongodb'
        itemsAfter[2]['systemVersion'] == '3.4'
        itemsAfter[2]['url'] == 'https://mongodb.thehyve.net:5467'
        itemsAfter[2]['singleFileCollections'] == true
    }

    def linkUpdateTest() {
        when:
        def indexResponse = get("/$VERSION/files")
        int fileId = indexResponse.json[FILES_COLLECTION_NAME][0].id
        def bodyContent = ['name'        : 'updated name',
                           'sourceSystem': testStorageSystem().id,
                           'study'       : 'storage_study2',
                           'uuid'        : 'aaaaa-bbbbb-ccccccccccccccc'] as JSON
        def before = get "/$VERSION/files/$fileId"
        def updateResponse = update "/$VERSION/files/$fileId", {
            contentType "application/json"
            json bodyContent
        }
        def after = get "/$VERSION/files/$fileId"
        def bodyJsn = before.json as JSON
        def putBack = update "/$VERSION/files/$fileId", {
            contentType "application/json"
            json bodyJsn
        }
        then:
        before.status == 200
        updateResponse.status == 200
        after.status == 200
        putBack.status == 200
        after.json['name'] == 'updated name'
        putBack.json['name'] == before.json['name']
    }

    def storageSystemGetTest() {
        when:
        def indexResponse = get("/$VERSION/storage")
        int storageId = indexResponse.json[STORAGE_SYSTEM_COLLECTION_NAME][0].id
        def response = get("/$VERSION/storage/$storageId")
        then:
        response.status == 200
        response.json['name'] == 'arvados keep at The Hyve'
        response.json['systemType'] == 'arvados'
        response.json['systemVersion'] == 'v1'
        response.json['url'] == 'https://arvbox-pro-dev.thehyve.net'
        response.json['singleFileCollections'] == false
    }

    def storageSystemDeleteTest() {
        when:
        def storageId = testStorageSystem2().id
        def beforeResponse = get("/$VERSION/storage/${storageId}")
        def deleteResponse = delete("/$VERSION/storage/${storageId}")
        def afterResponse = get("/$VERSION/storage/${storageId}")
        then:
        beforeResponse.status == 200
        deleteResponse.status == 204
        afterResponse.status == 404
    }

}
