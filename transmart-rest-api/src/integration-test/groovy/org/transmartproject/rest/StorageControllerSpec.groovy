package org.transmartproject.rest

import grails.converters.JSON
import grails.web.mime.MimeType

import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that

/**
 * Created by piotrzakrzewski on 09/12/2016.
 */
class StorageControllerSpec extends ResourceSpec {

    public static final String VERSION = "v2"

    void storageIndexTest() {
        when:
        def response = get "/$VERSION/files"
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

    void postFileLinkTest() {
        when:
        def bodyContent = ['name'        : 'new file Link',
                           'sourceSystem': 1,
                           'study'       : 'STUDY_ID_1',
                           'uuid'        : 'aaaaa-bbbbb-ccccccccccccccc'] as JSON
        def response = post "/$VERSION/files", {
            contentType "application/json"
            json bodyContent
        }
        then:
        response.status == 201
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
}
