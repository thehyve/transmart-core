/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest.v2

import grails.converters.JSON
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity

import static org.springframework.http.HttpStatus.BAD_REQUEST
import static org.springframework.http.HttpStatus.OK
import static org.transmartproject.rest.utils.ResponseEntityUtils.toJson

class QueryControllerSpec extends V2ResourceSpec {

    void 'test JSON (de)serialisation'() {
        def constraint = [
                type    : 'field',
                operator: '=',
                field   : [
                        dimension: 'patient',
                        fieldName: 'id',
                        type     : 'ID'
                ],
                value   : -101
        ] as JSON
        def constraintJSON = constraint.toString(false)
        def url = "${contextPath}/observations?type=clinical&constraint=${URLEncoder.encode(constraintJSON, 'UTF-8')}"

        when:
        ResponseEntity<Resource> response = get(url)
        def result = toJson(response)

        then:
        response.statusCode == OK
        result instanceof Map
    }

    void 'test_sorting'() {
        def constraint = URLEncoder.encode(([
                type: 'true',
        ] as JSON).toString(false), 'UTF-8')

        when:
        def sort = [dimension: dimension] as Map
        if (order != 'asc')
            sort.sortOrder = order
        def sortJson = URLEncoder.encode((
                [sort] as JSON
        ).toString(false), 'UTF-8')
        def url = "${contextPath}/observations?type=clinical&" +
                "constraint=$constraint&" +
                "sort=$sortJson"
        ResponseEntity<Resource> response = get(url)

        then:
        response.statusCode == OK
        def result = toJson(response)
        int dimIndex = result.dimensionDeclarations.findIndexOf { it.name == dimension }
        dimIndex > 0
        List elements = result.dimensionElements[dimension]
        List observationElements = result.cells.collect { elements[it.dimensionIndexes[dimIndex]] }
        List keys = observationElements*.getAt(dimensionKey)
        keys == (order == 'asc' ? keys.sort(false) : keys.sort(false).reverse())

        where:
        dimension | dimensionKey  | order
        'patient' | 'id'          | 'asc'
        'patient' | 'id'          | 'desc'
        'concept' | 'conceptCode' | 'asc'
    }

    void 'test invalid constraint'() {
        // invalid constraint with an operator that is not supported for the value type.
        def constraint = [
                type     : 'value',
                operator : '<',
                valueType: 'STRING',
                value    : 'invalid dummy value'
        ] as JSON

        when:
        def url = "${contextPath}/observations?type=clinical&constraint=${URLEncoder.encode(constraint.toString(false), 'UTF-8')}"

        ResponseEntity<Resource> response = get(url)
        def result = toJson(response)

        then:
        response.statusCode == BAD_REQUEST
        result.errors[0].message == "The value type is not compatible with the operator"
    }

    void 'test invalid JSON'() {
        def constraint = [
                type: 'true'
        ] as JSON

        when:
        def constraintJSON = constraint.toString(false)[0..-2] // remove last character of the JSON string
        def url = "${contextPath}/observations?type=clinical&constraint=${URLEncoder.encode(constraintJSON, 'UTF-8')}"

        ResponseEntity<Resource> response = get(url)
        def result = toJson(response)

        then:
        response.statusCode == BAD_REQUEST
        result.message.startsWith('Cannot parse constraint parameter: Unexpected end-of-input: expected close marker for Object')
    }

    void 'test getSupportedFields'() {
        when:
        def url = "${contextPath}/supported_fields"
        ResponseEntity<Resource> response = get(url)

        def result = toJson(response)

        then:
        response.statusCode == OK
        result instanceof List
    }

}
