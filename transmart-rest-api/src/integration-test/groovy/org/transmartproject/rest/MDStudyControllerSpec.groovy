/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.rest

import grails.converters.JSON
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.transmartproject.rest.marshallers.MarshallerSpec
import spock.lang.Ignore

@Slf4j
class MDStudyControllerSpec extends MarshallerSpec {

    public static final String VERSION = 'v2'

    void 'test get studies'() {
        when:
        def url = "${baseURL}/$VERSION/studies"
        ResponseEntity<Resource> response = getJson(url)

        String content = response.body.inputStream.readLines().join('\n')
        def result = new JsonSlurper().parseText(content)
        log.info 'Studies:'
        result['studies'].each {
            log.info (((Map)it).toMapString())
        }

        then:
        response.statusCode.value() == 200
        result['studies'] instanceof List
    }

    void 'test get study by id'() {
        when:
        def url = "${baseURL}/$VERSION/studies"
        ResponseEntity<Resource> response = getJson(url)

        String content = response.body.inputStream.readLines().join('\n')
        def result = new JsonSlurper().parseText(content)
        log.info 'Studies:'
        result['studies'].each {
            log.info (((Map)it).toMapString())
        }

        then:
        response.statusCode.value() == 200
        result['studies'] instanceof List

        when:
        def id = result['studies'][0]['id']
        def studyUrl = "${baseURL}/$VERSION/studies/${id}"
        ResponseEntity<Resource> studyResponse = getJson(studyUrl)
        String studyContent = studyResponse.body.inputStream.readLines().join('\n')
        def studyResult = new JsonSlurper().parseText(studyContent)
        log.info 'Study:'
        log.info (((Map)studyResult).toMapString())

        then:
        studyResponse.statusCode.value() == 200
        studyResult['id'] == id
    }

    void 'test get study by study id'() {
        when:
        def studyId = 'study1'
        def url = "${baseURL}/$VERSION/studies/studyId/${studyId}"
        ResponseEntity<Resource> response = getJson(url)

        String content = response.body.inputStream.readLines().join('\n')
        def result = new JsonSlurper().parseText(content)
        log.info 'Study:'
        log.info (((Map)result).toMapString())

        then:
        response.statusCode.value() == 200
        result['studyId'] == studyId
    }

    void 'test get non existing study by study id'() {
        when:
        def studyId = 'non existing study'
        def url = "${baseURL}/$VERSION/studies/studyId/${studyId}"
        ResponseEntity<Resource> response = getJson(url)

        String content = response.body.inputStream.readLines().join('\n')
        def result = new JsonSlurper().parseText(content)
        log.info 'Study:'
        log.info (((Map)result).toMapString())

        then:
        response.statusCode.value() == 403
    }

}
