package org.transmartproject.rest

import grails.converters.JSON

/**
 * Created by piotrzakrzewski on 15/12/2016.
 */
class ArvadosControllerSpec extends ResourceSpec {

    public static final String VERSION = "v2"
    public static final String COLLECTION_NAME = 'supportedWorkflows'

    void workflowsIndexTest() {
        when:
        def response = get "/$VERSION/arvados/workflows"
        then:
        response.status == 200
        response.json[COLLECTION_NAME]
        response.json[COLLECTION_NAME].size() == 1
        response.json[COLLECTION_NAME][0].name == "example workflow"
        response.json[COLLECTION_NAME][0].description == "This workflow exemplifies the aptness of this solution"
        response.json[COLLECTION_NAME][0].arvadosInstanceUrl == "https://arvbox-pro-dev.thehyve.net:8000"
        response.json[COLLECTION_NAME][0].arvadosVersion == "v1"
        response.json[COLLECTION_NAME][0].defaultParams == "{'firstParam':2, 'secondParam':'bla'}"
    }

    void postWorkflow() {
        when:
        def bodyContent = ['name'              : 'new file Link',
                           'description '      : 'description',
                           'arvadosInstanceUrl': 'http://arv-server.com',
                           'arvadosVersion'    : 'v1',
                           'defaultParams'     : "{'firstParam':10, 'secondParam':'maraP'}"] as JSON
        def postResponse = post "/$VERSION/arvados/workflows", {
            contentType "application/json"
            json bodyContent
        }
        def getResponse = get "/$VERSION/arvados/workflows/${postResponse.json.id}"
        then:
        postResponse.status == 201
        getResponse.status == 200
        getResponse.json
        getResponse.json.name == 'new file Link'
        getResponse.json.uuid == 'aaaaa-bbbbb-ccccccccccccccc'
        getResponse.json.study == 'storage_study2'
        getResponse.json.sourceSystem == 1
    }

    def singleGetTest() {
        when:
        def response = get("/$VERSION/arvados/workflows/1")
        then:
        response.status == 200
        response.json['name'] == '1000 genemes VCFs'
        response.json['uuid'] == 'ys8ib-4zz18-cyw4o6pmrxrixnr'
        response.json['study'] == 'storage_study'
        response.json['sourceSystem'] == 1
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
        response.json[expectedCollectionName][0].sourceSystem == 1
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

    def LinkUpdateTest() {
        when:
        def bodyContent = ['name'        : 'updated name',
                           'sourceSystem': 1,
                           'study'       : 'storage_study2',
                           'uuid'        : 'aaaaa-bbbbb-ccccccccccccccc'] as JSON
        def before = get "/$VERSION/files/1"
        def updateResponse = update "/$VERSION/files/1", {
            contentType "application/json"
            json bodyContent
        }
        def after = get "/$VERSION/files/1"
        def bodyJsn = before.json as JSON
        def putBack = update "/$VERSION/files/1", {
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
