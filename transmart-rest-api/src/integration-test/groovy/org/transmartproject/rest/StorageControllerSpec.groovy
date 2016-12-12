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
        response.json[FILES_COLLECTION_NAME][0].studyId == 'STUDY_ID_1'
        response.json[FILES_COLLECTION_NAME][0].sourceSystemId == 1
    }

    void postFileLinkTest() {
        when:
        def bodyContent = ['name'        : 'new file Link',
                           'sourceSystem': 1,
                           'study'       : 'STUDY_ID_2',
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
        indexResponse.json[FILES_COLLECTION_NAME][1].studyId == 'STUDY_ID_2'
        indexResponse.json[FILES_COLLECTION_NAME][1].sourceSystemId == 1
    }

    def singleGetTest() {
        when:
        def response = get("/$VERSION/files/1")
        then:
        response.status == 200
        response.json['name'] == '1000 genemes VCFs'
        response.json['uuid'] == 'ys8ib-4zz18-cyw4o6pmrxrixnr'
        response.json['studyId'] == 'STUDY_ID_1'
        response.json['sourceSystemId'] == 1
    }

    void indexByStudyTest() {
        when:
        def response = get "/$VERSION/studies/STUDY_ID_1/files"
        String expectedCollectionName = 'files'
        then:
        response.status == 200
        response.json[expectedCollectionName]
        response.json[expectedCollectionName].size() == 1
        response.json[expectedCollectionName][0].name == '1000 genemes VCFs'
        response.json[expectedCollectionName][0].uuid == 'ys8ib-4zz18-cyw4o6pmrxrixnr'
        response.json[expectedCollectionName][0].studyId == 'STUDY_ID_1'
        response.json[expectedCollectionName][0].sourceSystemId == 1
    }

    void StorageSystemPosttest() {
        when:
        def bodyContent = [
                'name':'mongodb at The Hyve',
                'systemType':'mongodb',
                'url':'https://mongodb.thehyve.net:5467',
                'systemVersion':'3.4',
                'singleFileCollections':true,
        ] as JSON
        def response = post "/$VERSION/storage", {
            contentType "application/json"
            json bodyContent
        }
        def indexResponse = get("/$VERSION/storage")
        then:
        response.status == 201
        indexResponse.status == 200
        indexResponse.json[STORAGE_SYSTEM_COLLECTION_NAME].size() == 2
        indexResponse.json[STORAGE_SYSTEM_COLLECTION_NAME][1]['name'] == 'mongodb at The Hyve'
        indexResponse.json[STORAGE_SYSTEM_COLLECTION_NAME][1]['systemType'] == 'mongodb'
        indexResponse.json[STORAGE_SYSTEM_COLLECTION_NAME][1]['systemVersion'] == '3.4'
        indexResponse.json[STORAGE_SYSTEM_COLLECTION_NAME][1]['url'] == 'https://mongodb.thehyve.net:5467'
        indexResponse.json[STORAGE_SYSTEM_COLLECTION_NAME][1]['singleFileCollections'] == true
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
}
