/* (c) Copyright 2019, The Hyve. */

package org.transmartproject.rest.v2

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.transmartproject.core.multidimquery.hypercube.DimensionProperties
import org.transmartproject.mock.MockUser
import org.transmartproject.rest.MimeTypes
import org.transmartproject.rest.data.AccessPolicyTestData

import static org.springframework.http.HttpStatus.BAD_REQUEST
import static org.springframework.http.HttpStatus.CREATED
import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.OK
import static org.transmartproject.rest.utils.ResponseEntityUtils.*

class DimensionControllerSpec extends V2ResourceSpec {

    @Autowired
    AccessPolicyTestData testData

    void setup() {
        selectUser(new MockUser('test', true))
        testData.clearTestData()
        testData.createTestData()
    }

    void 'test get dimensions'() {
        when:
        def url = "${contextPath}/dimensions"
        ResponseEntity<Resource> response = get(url)


        then:
        response.statusCode == OK
        def result = toJson(response)
        result['dimensions'] instanceof List
        result['dimensions'].collect { it.name } as Set == ['study', 'end time', 'start time', 'concept', 'patient', 'trial visit'] as Set
    }

    void 'test get dimension by name'() {
        when:
        def url = "${contextPath}/dimensions/patient"
        ResponseEntity<Resource> response = get(url)

        then:
        response.statusCode == OK
        def result = toObject(response, DimensionProperties)
        result.name == 'patient'
    }

    void 'test get non existing dimension'() {
        when:
        def url = "${contextPath}/dimensions/none"
        ResponseEntity<Resource> response = get(url)

        then:
        response.statusCode == NOT_FOUND
    }

    void 'test get patient dimension elements'() {
        when:
        def url = "${contextPath}/dimensions/patient/elements"
        ResponseEntity<Resource> response = post(url, [:])

        then:
        response.statusCode == OK
        def result = toJson(response)
        result['elements'] instanceof List
        result['elements'].collect { it.subjectIds['SUBJ_ID'] } == ['Subject 1', 'Subject 2', 'Subject 3', 'Subject from public study']
    }

    void 'test get patient dimension elements for patient set constraint'() {
        when:
        def patientSetUrl = "${contextPath}/patient_sets?name=test"
        ResponseEntity<Resource> patientSetresponse = post(patientSetUrl, MimeTypes.APPLICATION_JSON, MimeTypes.APPLICATION_JSON,
                [type: 'study_name', studyId: 'study1'])
        then:
        patientSetresponse.statusCode == CREATED
        def patientSetResult = toJson(patientSetresponse)
        patientSetResult['setSize'] == 2
        def patientSetId = patientSetResult['id']

        when:
        def url = "${contextPath}/dimensions/patient/elements"
        ResponseEntity<Resource> response = post(
                url,
                MimeTypes.APPLICATION_JSON,
                MimeTypes.APPLICATION_JSON,
                [constraint: [type: 'patient_set', patientSetId: patientSetId]]
        )

        then:
        response.statusCode == OK
        def result = toJson(response)
        result['elements'] instanceof List
        result['elements'].collect { it.subjectIds['SUBJ_ID'] } == ['Subject 1', 'Subject 2']
    }

    void 'test get concept dimension elements'() {
        when:
        def url = "${contextPath}/dimensions/concept/elements"
        ResponseEntity<Resource> response = post(url, [:])

        then:
        response.statusCode == OK
        def result = toJson(response)
        result['elements'] instanceof List
        result['elements'].collect { it.conceptCode } == ['categorical_concept1', 'numerical_concept2']
    }

    void 'test get study dimension elements'() {
        when:
        def url = "${contextPath}/dimensions/study/elements"
        ResponseEntity<Resource> response = post(url, [:])

        then:
        response.statusCode == OK
        def result = toJson(response)
        result['elements'] instanceof List
        result['elements'].collect { it.name } == ['publicStudy', 'study1', 'study2']
    }

    void 'test get study dimension elements for constraint'() {
        when:
        def url = "${contextPath}/dimensions/study/elements"
        ResponseEntity<Resource> response = post(
                url,
                MimeTypes.APPLICATION_JSON,
                MimeTypes.APPLICATION_JSON,
                [constraint: [type: 'study_name', studyId: 'study2']]
        )

        then:
        response.statusCode == OK
        def result = toJson(response)
        result['elements'] instanceof List
        result['elements'].collect { it.name } == ['study2']
    }

    void 'test get dimension elements for invalid constraint'() {
        when:
        def url = "${contextPath}/dimensions/study/elements"
        ResponseEntity<Resource> response = post(
                url,
                MimeTypes.APPLICATION_JSON,
                MimeTypes.APPLICATION_JSON,
                [constraint: [invalid: 'invalid']]
        )

        then:
        response.statusCode == BAD_REQUEST
    }

}
