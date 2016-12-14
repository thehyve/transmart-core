package org.transmartproject.rest

import grails.converters.JSON

/**
 * Created by piotrzakrzewski on 09/12/2016.
 */
class StorageControllerSpec extends ResourceSpec {

    public static final String VERSION = "v2"
    public static final String FILES_COLLECTION_NAME = 'files'
    public static final String STORAGE_SYSTEM_COLLECTION_NAME = 'storageSystems'

    void storageIndexTest() {
        when:
        def response = get "/$VERSION/files"
        then:
        response.status == 200
        response.json[FILES_COLLECTION_NAME]
        response.json[FILES_COLLECTION_NAME].size() == 1
        response.json[FILES_COLLECTION_NAME][0].name == '1000 genemes VCFs'
        response.json[FILES_COLLECTION_NAME][0].uuid == 'ys8ib-4zz18-cyw4o6pmrxrixnr'
        response.json[FILES_COLLECTION_NAME][0].studyId == 'storage_study'
        response.json[FILES_COLLECTION_NAME][0].sourceSystemId == 1
    }

    void postFileLinkTest() {
        when:
        def bodyContent = ['name'        : 'new file Link',
                           'sourceSystem': 1,
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
        indexResponse.json[FILES_COLLECTION_NAME][1].studyId == 'storage_study2'
        indexResponse.json[FILES_COLLECTION_NAME][1].sourceSystemId == 1
    }

    def singleGetTest() {
        when:
        def response = get("/$VERSION/files/1")
        then:
        response.status == 200
        response.json['name'] == '1000 genemes VCFs'
        response.json['uuid'] == 'ys8ib-4zz18-cyw4o6pmrxrixnr'
        response.json['studyId'] == 'storage_study'
        response.json['sourceSystemId'] == 1
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
        response.json[expectedCollectionName][0].studyId == 'storage_study'
        response.json[expectedCollectionName][0].sourceSystemId == 1
    }

    void StorageSystemPostTest() {
        when:
        def indexResponseBefore = get("/$VERSION/storage")
        def bodyContent = [
                'name'                 : 'mongodb at The Hyve',
                'systemType'           : 'mongodb',
                'url'                  : 'https://mongodb.thehyve.net:5467',
                'systemVersion'        : '3.4',
                'singleFileCollections': true,
        ] as JSON
        def response = post "/$VERSION/storage", {
            contentType "application/json"
            json bodyContent
        }
        def indexResponseAfter = get("/$VERSION/storage")
        int itemsBefore = indexResponseBefore.json[STORAGE_SYSTEM_COLLECTION_NAME].size()
        int itemsAfter = indexResponseAfter.json[STORAGE_SYSTEM_COLLECTION_NAME].size()
        then:
        response.status == 201
        indexResponseAfter.status == 200
        indexResponseBefore.status == 200
        itemsAfter == (itemsBefore + 1)
        indexResponseAfter.json[STORAGE_SYSTEM_COLLECTION_NAME][2]['name'] == 'mongodb at The Hyve'
        indexResponseAfter.json[STORAGE_SYSTEM_COLLECTION_NAME][2]['systemType'] == 'mongodb'
        indexResponseAfter.json[STORAGE_SYSTEM_COLLECTION_NAME][2]['systemVersion'] == '3.4'
        indexResponseAfter.json[STORAGE_SYSTEM_COLLECTION_NAME][2]['url'] == 'https://mongodb.thehyve.net:5467'
        indexResponseAfter.json[STORAGE_SYSTEM_COLLECTION_NAME][2]['singleFileCollections'] == true
    }

    def storageSystemGetTest() {
        when:
        def response = get("/$VERSION/storage/1")
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
        def beforeResponse = get("/$VERSION/storage/2")
        def deleteResponse = delete("/$VERSION/storage/2")
        def afterResponse = get("/$VERSION/storage/2")
        then:
        beforeResponse.status == 200
        deleteResponse.status == 204
        afterResponse.status == 404
    }

}
