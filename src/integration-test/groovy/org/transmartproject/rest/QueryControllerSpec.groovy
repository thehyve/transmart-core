package org.transmartproject.rest

import grails.converters.JSON
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.transmartproject.rest.marshallers.MarshallerSpec
import spock.lang.Ignore

@Slf4j
class QueryControllerSpec extends MarshallerSpec {

    public static final String VERSION = 'v2'

    @Ignore // FIXME: This test fails for some reason
    void 'test JSON (de)serialisation'() {
        def constraint = [
                type: 'FieldConstraint',
                operator: '=',
                field: [
                        dimension: 'PatientDimension',
                        fieldName: 'id',
                        type: 'ID'
                ],
                value: -101
        ] as JSON
        log.info "Constraint: ${constraint.toString()}"

        when:
        def constraintJSON = constraint.toString(false)
        def url = "${baseURL}/$VERSION/observation_list?constraint=${URLEncoder.encode(constraintJSON, 'UTF-8')}"
        log.info "Request URL: ${url}"
        ResponseEntity<Resource> response = getJson(url)
        String content = response.body.inputStream.readLines().join('\n')
        def result = new JsonSlurper().parseText(content)

        then:
        response.statusCode.value() == 200
        result instanceof List
    }

    void 'test invalid constraint'() {
        // invalid constraint with an operator that is not supported for the value type.
        def constraint = [
                type: 'ValueConstraint',
                operator: '<',
                valueType: 'STRING',
                value: 'invalid dummy value'
        ] as JSON
        log.info "Constraint: ${constraint.toString(false)}"

        when:
        def url = "${baseURL}/$VERSION/observation_list?constraint=${URLEncoder.encode(constraint.toString(false), 'UTF-8')}"
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
                type: 'TrueConstraint'
        ] as JSON
        log.info "Constraint: ${constraint.toString()}"

        when:
        def constraintJSON = constraint.toString(false)[0..-2] // remove last character of the JSON string
        log.info "Invalid JSON: ${constraintJSON}"
        def url = "${baseURL}/$VERSION/observation_list?constraint=${URLEncoder.encode(constraintJSON, 'UTF-8')}"
        log.info "Request URL: ${url}"

        ResponseEntity<Resource> response = getJson(url)
        String content = response.body.inputStream.readLines().join('\n')
        def result = new JsonSlurper().parseText(content)

        then:
        response.statusCode.value() == 400
        result.message == 'Cannot parse constraint parameter.'
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

}
