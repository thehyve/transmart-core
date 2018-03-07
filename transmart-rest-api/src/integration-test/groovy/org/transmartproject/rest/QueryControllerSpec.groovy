/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import grails.converters.JSON
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.transmartproject.db.TestData
import org.transmartproject.rest.marshallers.MarshallerSpec
import spock.lang.Ignore

@Slf4j
class QueryControllerSpec extends MarshallerSpec {

    public static final String VERSION = 'v2'

    @Ignore
    void 'test JSON (de)serialisation'() {
        def constraint = [
                type: 'field',
                operator: '=',
                field: [
                        dimension: 'patient',
                        fieldName: 'id',
                        type: 'ID'
                ],
                value: -101
        ] as JSON
        log.info "Constraint: ${constraint.toString()}"

        when:
        def constraintJSON = constraint.toString(false)
        def url = "${baseURL}/$VERSION/observations?type=clinical&constraint=${URLEncoder.encode(constraintJSON, 'UTF-8')}"
        log.info "Request URL: ${url}"
        ResponseEntity<Resource> response = getJson(url)
        String content = response.body.inputStream.readLines().join('\n')
        def result = new JsonSlurper().parseText(content)

        then:
        response.statusCode.value() == 200
        result instanceof Map
    }

    void 'test invalid constraint'() {
        // invalid constraint with an operator that is not supported for the value type.
        def constraint = [
                type: 'value',
                operator: '<',
                valueType: 'STRING',
                value: 'invalid dummy value'
        ] as JSON
        log.info "Constraint: ${constraint.toString(false)}"

        when:
        def url = "${baseURL}/$VERSION/observations?type=clinical&constraint=${URLEncoder.encode(constraint.toString(false), 'UTF-8')}"
        log.info "Request URL: ${url}"

        ResponseEntity<Resource> response = getJson(url)
        String content = response.body.inputStream.readLines().join('\n')
        def result = new JsonSlurper().parseText(content)

        then:
        response.statusCode.value() == 400
        result.errors[0].message == "Operator [<] not valid for type STRING"
    }

    void 'test invalid JSON'() {
        def constraint = [
                type: 'true'
        ] as JSON
        log.info "Constraint: ${constraint.toString()}"

        when:
        def constraintJSON = constraint.toString(false)[0..-2] // remove last character of the JSON string
        log.info "Invalid JSON: ${constraintJSON}"
        def url = "${baseURL}/$VERSION/observations?type=clinical&constraint=${URLEncoder.encode(constraintJSON, 'UTF-8')}"
        log.info "Request URL: ${url}"

        ResponseEntity<Resource> response = getJson(url)
        String content = response.body.inputStream.readLines().join('\n')
        def result = new JsonSlurper().parseText(content)

        then:
        response.statusCode.value() == 400
        result.message == "Cannot parse constraint parameter: $constraintJSON"
    }

    void 'test getSupportedFields'() {
        when:
        def url = "${baseURL}/$VERSION/supported_fields"
        ResponseEntity<Resource> response = getJson(url)

        String content = response.body.inputStream.readLines().join('\n')
        def result = new JsonSlurper().parseText(content)
        if (result instanceof List) {
            log.info 'Supported fields:'
            result.each {
                log.info (((Map)it).toMapString())
            }
        }

        then:
        response.statusCode.value() == 200
        result instanceof List
    }

    void 'test pack parameter'() {
        when:
        def constraintTxt = URLEncoder.encode(([
                type: 'negation',
                arg: [
                        type: 'true'
                ]
        ] as JSON).toString(), 'UTF-8')
        def response = getProtobuf("$baseURL/$VERSION/observations?type=clinical&constraint=$constraintTxt")

        then:
        response.statusCode.value() == 400
        new JsonSlurper().parse(response.body.byteArray).message ==
                "Parameter 'pack' is required for protobuf output, currently only the value 'f' is supported."

        when:
        // Only as long as packing is not implemented
        response = getProtobuf("$baseURL/$VERSION/observations?type=clinical&pack=t&constraint=$constraintTxt")
        then:
        response.statusCode.value() == 400
        new JsonSlurper().parse(response.body.byteArray).message ==
                "Parameter 'pack' is required for protobuf output, currently only the value 'f' is supported."

        when:
        response = getProtobuf("$baseURL/$VERSION/observations?type=clinical&pack=f&constraint=$constraintTxt")
        then:
        response.statusCode.value() == 200
    }
}
