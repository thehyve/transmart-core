/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest.v2

import groovy.json.JsonBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.transmartproject.mock.MockUser
import org.transmartproject.rest.data.V1DefaultTestData

import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.OK
import static org.transmartproject.rest.utils.ResponseEntityUtils.toJson

class MDStudyControllerSpec extends V2ResourceSpec {

    @Autowired
    V1DefaultTestData testData

    void setup() {
        selectUser(new MockUser('test', true))
        testData.clearTestData()
        testData.createTestData()
    }

    void 'test get studies'() {
        when:
        def url = "${contextPath}/studies"
        ResponseEntity<Resource> response = get(url)


        then:
        response.statusCode == OK
        def result = toJson(response)
        result['studies'] instanceof List
    }

    void 'test get study by id'() {
        when:
        def url = "${contextPath}/studies"
        ResponseEntity<Resource> response = get(url)

        then:
        response.statusCode == OK
        def result = toJson(response)
        result['studies'] instanceof List

        when:
        def id = result['studies'][0]['id']
        def studyUrl = "${contextPath}/studies/${id}"
        ResponseEntity<Resource> studyResponse = get(studyUrl)

        then:
        def studyResult = toJson(studyResponse)
        studyResponse.statusCode == OK
        studyResult['id'] == id
    }

    void 'test get study by study id'() {
        when:
        def studyId = 'study1'
        def url = "${contextPath}/studies/studyId/${studyId}"
        ResponseEntity<Resource> response = get(url)

        then:
        response.statusCode == OK
        def result = toJson(response)
        result['studyId'] == studyId
    }

    void 'test get multiple studies by study ids'() {
        when:
        def studyId1 = 'study1'
        def studyId2 = 'study2'
        def studyIds = new JsonBuilder([studyId1, studyId2]).toString()
        def url = "${contextPath}/studies/studyIds?studyIds=$studyIds"
        ResponseEntity<Resource> response = get(url)


        then:
        response.statusCode == OK
        def result = toJson(response)
        result['studies'].size() == 2
        result['studies']['studyId'] == [studyId1, studyId2]
    }

    void 'test get non existing study by study id'() {
        when:
        def studyId = 'non existing study'
        def url = "${contextPath}/studies/studyId/${studyId}"
        ResponseEntity<Resource> response = get(url)

        then:
        def result = toJson(response)
        response.statusCode == NOT_FOUND
    }

}
