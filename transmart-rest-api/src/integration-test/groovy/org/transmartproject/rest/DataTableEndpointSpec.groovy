/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.transmartproject.rest.marshallers.MarshallerSpec

@Slf4j
class DataTableEndpointSpec extends MarshallerSpec {

    public static final String VERSION = 'v2'

    void 'test data table'() {
        def constraint = [
                type: 'true',
        ]

        when:
        def columnSort = [[dimension: 'concept', sortOrder: 'desc']]
        def rowDimensions = ['patient', 'study']
        def columnDimensions = ['trial visit', 'concept']
        def limit = 4
        def offset = 0

        def url = "${baseURL}/$VERSION/observations/table"
        def body = [
                type: 'clinical',
                constraint: constraint,
                rowDimensions: rowDimensions,
                columnDimensions: columnDimensions,
                columnSort: columnSort,
                limit: limit,
                offset: offset
        ]
        log.info "Request URL: ${url}"
        ResponseEntity<Resource> response = postJson(url, body)
        String content = response.body.inputStream.readLines().join('\n')

        then:
        response.statusCode.value() == 200
        def result = new JsonSlurper().parseText(content)
        result.offset == offset
        result.columnDimensions*.name == ['trial visit', 'concept']
        result.columnHeaders*.dimension == ['trial visit', 'concept']
        result.rows.size() == 4
        result.rowCount == 4
        result.sort.find{ it.dimension == 'study'}.sortOrder == "asc"
        result.sort.find{ it.dimension == 'patient'}.sortOrder == "asc"
        result.sort.find{ it.dimension == 'concept'}.sortOrder == "desc"

        when:
        def offset2 = 2
        def limit2 = 2
        body.limit = limit2
        body.offset = offset2
        ResponseEntity<Resource> response2 = postJson(url, body)
        String content2 = response2.body.inputStream.readLines().join('\n')

        then:
        response2.statusCode.value() == 200
        def result2 = new JsonSlurper().parseText(content2)
        result2.offset == offset2
        result2.rows.size() == 2
        result2.rowCount == 4
        result2.rows*.dimensions == result.rows.takeRight(2)*.dimensions
        result2.rows*.row*.findAll() == result.rows.takeRight(2)*.row*.findAll()

        when: "test without dimensions"
        body = [
                type: 'clinical',
                constraint: constraint,
                rowDimensions: ['study'],
                columnDimensions: [],
                limit: limit,
                offset: offset
        ]
        ResponseEntity<Resource> response3 = postJson(url, body)
        String content3 = response3.body.inputStream.readLines().join('\n')
        def result3 = new JsonSlurper().parseText(content3)

        then:
        result3.offset == 0
        result3.rowCount == 1
        result3.columnHeaders*.dimension == []
        result3.rows[0].cells.size() == 1
        result3.rows[0].cells[0].size() > 1
    }

}
