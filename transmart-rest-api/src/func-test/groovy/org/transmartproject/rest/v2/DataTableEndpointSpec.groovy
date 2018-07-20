/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest.v2

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.transmartproject.mock.MockUser
import org.transmartproject.rest.data.V1DefaultTestData

import static org.springframework.http.HttpStatus.OK
import static org.transmartproject.rest.utils.ResponseEntityUtils.toJson

@Slf4j
class DataTableEndpointSpec extends V2ResourceSpec {

    @Autowired
    V1DefaultTestData testData

    void setup() {
        selectUser(new MockUser('test', true))
        testData.clearTestData()
        testData.createTestData()
    }

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

        def url = "${contextPath}/observations/table"
        def body = [
                type            : 'clinical',
                constraint      : constraint,
                rowDimensions   : rowDimensions,
                columnDimensions: columnDimensions,
                columnSort      : columnSort,
                limit           : limit,
                offset          : offset
        ]
        log.info "Request URL: ${url}"
        ResponseEntity<Resource> response = post(url, body)

        then:
        response.statusCode == OK
        def result = toJson(response)
        result.offset == offset
        result.columnDimensions*.name == ['trial visit', 'concept']
        result.columnHeaders*.dimension == ['trial visit', 'concept']
        result.rows.size() == 4
        result.rowCount == 4
        result.sort.find { it.dimension == 'study' }.sortOrder == "asc"
        result.sort.find { it.dimension == 'patient' }.sortOrder == "asc"
        result.sort.find { it.dimension == 'concept' }.sortOrder == "desc"

        when:
        def offset2 = 2
        def limit2 = 2
        body.limit = limit2
        body.offset = offset2
        ResponseEntity<Resource> response2 = post(url, body)

        then:
        response2.statusCode == OK
        def result2 = toJson(response2)
        result2.offset == offset2
        result2.rows.size() == 2
        result2.rowCount == 4
        result2.rows*.dimensions == result.rows.takeRight(2)*.dimensions
        result2.rows*.row*.findAll() == result.rows.takeRight(2)*.row*.findAll()

        when: "test without dimensions"
        body = [
                type            : 'clinical',
                constraint      : constraint,
                rowDimensions   : ['study'],
                columnDimensions: [],
                limit           : limit,
                offset          : offset
        ]
        ResponseEntity<Resource> response3 = post(url, body)

        then:
        def result3 = toJson(response3)
        result3.offset == 0
        result3.rowCount == 1
        result3.columnHeaders*.dimension == []
        result3.rows[0].cells.size() == 1
        result3.rows[0].cells[0].size() > 1
    }

}
