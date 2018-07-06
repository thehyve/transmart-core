/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import grails.converters.JSON

/**
 * Created by piotrzakrzewski on 15/12/2016.
 */
class ArvadosControllerSpec extends ResourceSpec {

    public static final String VERSION = "v2"
    public static final String COLLECTION_NAME = 'supportedWorkflows'

    void workflowsIndexTest() {
        given:
        testDataSetup()

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
        response.json[COLLECTION_NAME][0].defaultParams == ['firstParam': 2, 'secondParam': 'bla']
        response.json[COLLECTION_NAME][0].uuid == "qwuip-ouip-aaaaaaaaaaaaaa"
    }

    void postWorkflow() {
        when:
        def bodyContent = ['name'              : 'new workflow',
                           'description'       : 'description',
                           'arvadosInstanceUrl': 'http://arv-server.com',
                           'arvadosVersion'    : 'v1',
                           'defaultParams'     : ["firstParam":10, "secondParam":"maraP"],
                           'uuid'              : 'instance-objid-randomstr',] as JSON
        def postResponse = post "/$VERSION/arvados/workflows", {
            contentType "application/json"
            json bodyContent
        }
        def getResponse = get "/$VERSION/arvados/workflows/${postResponse.json.id}"

        then:
        postResponse.status == 201
        getResponse.status == 200
        getResponse.json
        getResponse.json.name == 'new workflow'
        getResponse.json.description == 'description'
        getResponse.json.uuid == 'instance-objid-randomstr'
        getResponse.json.defaultParams == ["firstParam":10, "secondParam":"maraP"]
        getResponse.json.arvadosVersion == 'v1'
        getResponse.json.uuid == 'instance-objid-randomstr'
        getResponse.json.arvadosInstanceUrl == 'http://arv-server.com'
    }

}
