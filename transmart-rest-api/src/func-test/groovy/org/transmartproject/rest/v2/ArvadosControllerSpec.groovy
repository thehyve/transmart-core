/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest.v2

import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.mock.MockUser
import org.transmartproject.rest.data.V1DefaultTestData

import static org.springframework.http.HttpStatus.CREATED
import static org.springframework.http.HttpStatus.OK
import static org.transmartproject.rest.MimeTypes.APPLICATION_JSON
import static org.transmartproject.rest.utils.ResponseEntityUtils.toJson

/**
 * Created by piotrzakrzewski on 15/12/2016.
 */
class ArvadosControllerSpec extends V2ResourceSpec {

    public static final String COLLECTION_NAME = 'supportedWorkflows'

    @Autowired
    V1DefaultTestData testData

    void setup() {
        selectUser(new MockUser('test', true))
        testData.clearTestData()
        testData.createTestData()
    }

    void workflowsIndexTest() {
        when:
        def response = get "${contextPath}/arvados/workflows"

        then:
        response.statusCode == OK
        def json = toJson(response)
        def collections = json[COLLECTION_NAME]
        collections.size() == 1
        def collection = collections[0]
        collection.name == "example workflow"
        collection.description == "This workflow exemplifies the aptness of this solution"
        collection.arvadosInstanceUrl == "https://arvbox-pro-dev.thehyve.net:8000"
        collection.arvadosVersion == "v1"
        collection.defaultParams == ['firstParam': 2, 'secondParam': 'bla']
        collection.uuid == "qwuip-ouip-aaaaaaaaaaaaaa"
    }

    void postWorkflow() {
        def bodyContent = ['name'              : 'new workflow',
                           'description'       : 'description',
                           'arvadosInstanceUrl': 'http://arv-server.com',
                           'arvadosVersion'    : 'v1',
                           'defaultParams'     : ["firstParam": 10, "secondParam": "maraP"],
                           'uuid'              : 'instance-objid-randomstr',]
        when:
        def postResponse = post("${contextPath}/arvados/workflows",
                APPLICATION_JSON,
                APPLICATION_JSON,
                bodyContent)
        then:
        postResponse.statusCode == CREATED

        when:
        def postJson = toJson(postResponse)
        def getResponse = get "${contextPath}/arvados/workflows/${postJson.id}"

        then:
        getResponse.statusCode == OK
        def json = toJson(getResponse)
        json.name == 'new workflow'
        json.description == 'description'
        json.uuid == 'instance-objid-randomstr'
        json.defaultParams == ["firstParam": 10, "secondParam": "maraP"]
        json.arvadosVersion == 'v1'
        json.uuid == 'instance-objid-randomstr'
        json.arvadosInstanceUrl == 'http://arv-server.com'
    }

}
