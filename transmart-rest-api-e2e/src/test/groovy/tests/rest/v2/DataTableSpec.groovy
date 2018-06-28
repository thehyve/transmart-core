/* Copyright © 2017 The Hyve B.V. */
package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.EqualsAndHashCode
import groovy.util.logging.Slf4j

import static base.ContentTypeFor.JSON
import static config.Config.*

@Slf4j
class DataTableSpec extends RESTSpec {

    @EqualsAndHashCode
    static class ColumnHeader {
        String dimension
        List<Object> elements
        List<Object> keys
    }

    @EqualsAndHashCode
    static class Dimension {
        String name
        Map<String, Object> elements
    }

    @EqualsAndHashCode
    static class RowHeader {
        String dimension
        Object element
        Object key
    }

    @EqualsAndHashCode
    static class Row {
        List<RowHeader> rowHeaders
        List<Object> cells
    }

    @EqualsAndHashCode
    static class SortSpecification {
        String dimension
        String sortOrder
        Boolean userRequested
    }

    @EqualsAndHashCode
    static class DataTable {
        List<ColumnHeader> columnHeaders
        List<Dimension> rowDimensions
        List<Dimension> columnDimensions
        Integer rowCount
        List<Row> rows
        Integer offset
        List<SortSpecification> sort
    }

    /**
     *  given: study EHR is loaded
     *  when: for that study I get all observations for heart rate in table format
     *  then: table is properly formatted
     */
    @RequiresStudy(EHR_ID)
    def 'get data table'() {
        given: 'study EHR is loaded'
        def limit = 10
        def params = [
                constraint: [
                        type: 'concept',
                        conceptCode: 'EHR:VSIGN:HR'
                ],
                type: 'clinical',
                rowDimensions: ['patient', 'study'],
                columnDimensions: ['trial visit', 'concept'],
                columnSort: [[dimension: 'trial visit', sortOrder: 'asc'], [dimension: 'concept', sortOrder: 'desc']],
                rowSort: [[dimension: 'patient', sortOrder: 'desc']],
                limit: limit,
        ]
        def request = [
                path: PATH_TABLE,
                acceptType: JSON,
                body: params
        ]

        when: 'for that study I get all observations for heart rate in table format'
        def mapper = new ObjectMapper()
        def responseData = post(request)
        String responseBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseData)
        DataTable dataTable = new ObjectMapper().readValue(responseBody, DataTable)

        then: 'table is properly formatted'
        dataTable.columnDimensions.size() == 2
        dataTable.columnHeaders.size() == 2
        dataTable.rowDimensions.size() == 2
        dataTable.rows.size() == 3
        dataTable.rowCount == 3
        dataTable.offset == 0
        dataTable.sort.find { it.dimension == 'study' }.sortOrder == 'asc'
        dataTable.sort.find { it.dimension == 'patient' }.sortOrder == 'desc'
        dataTable.sort.find { it.dimension == 'trial visit' }.sortOrder == 'asc'
        dataTable.sort.find { it.dimension == 'concept' }.sortOrder == 'desc'

        when: 'I specify an offset'
        def offset = 2
        limit = 2
        params.offset = offset
        params.limit = limit
        request.body = params
        def responseData2 = post(request)
        String responseBody2 = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseData2)
        DataTable dataTable2 = new ObjectMapper().readValue(responseBody2, DataTable)

        then: 'the number of results has decreased'
        dataTable2.offset == offset
        dataTable2.rowCount == dataTable.rowCount
        dataTable2.rows.size() == limit
        dataTable2.rows == dataTable.rows.takeRight(2)
    }

    /**
     *  given: study TUMOR_NORMAL_SAMPLES is loaded
     *  when: for that study I get all observations in table format
     *  then: the table contains the expected data
     */
    @RequiresStudy(TUMOR_NORMAL_SAMPLES_ID)
    def 'get data table with a modifier dimension'() {
        given: "study TUMOR_NORMAL_SAMPLES is loaded"
        def params = [
                constraint: [
                        type: 'study_name',
                        studyId: 'TUMOR_NORMAL_SAMPLES'
                ],
                type: 'clinical',
                rowDimensions: ['patient'],
                columnDimensions: ['concept', 'sample_type'],
                limit: 10
        ]
        def request = [
                path: PATH_TABLE,
                acceptType: JSON,
                body: params
        ]

        when: 'for that study I get all observations in table format'
        def mapper = new ObjectMapper()
        def responseData = post(request)
        String responseBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseData)
        log.info "RESPONSE: ${responseBody}"
        DataTable dataTable = new ObjectMapper().readValue(responseBody, DataTable)

        then: 'the table contains the expected data'
        dataTable.columnDimensions.size() == 2
        dataTable.columnDimensions
        dataTable.columnHeaders.size() == 2
        dataTable.columnHeaders.find { it.dimension == 'concept'}.keys ==
            ['TNS:DEM:AGE', 'TNS:HD:EXPBREAST', 'TNS:HD:EXPBREAST', 'TNS:HD:EXPLUNG', 'TNS:HD:EXPLUNG', 'TNS:LAB:CELLCNT', 'TNS:LAB:CELLCNT']
        dataTable.columnHeaders.find { it.dimension == 'sample_type'}.elements ==
            [null, 'Normal', 'Tumor', 'Normal', 'Tumor', 'Normal', 'Tumor']

        dataTable.rowDimensions.size() == 1
        dataTable.rowCount == 3
        dataTable.rows.size() == 3
        def patientDimensionElements = dataTable.rowDimensions.find { it.name == 'patient' }.elements
        def rowPatients = dataTable.rows.collect { patientDimensionElements[it.rowHeaders[0].key as String] }
        rowPatients.collect { it.subjectIds['SUBJ_ID'] } == ['TNS:63', 'TNS:53', 'TNS:43']
        // Patient TNS:63
        dataTable.rows[0].cells == [
                40, // Age
                null, // Breast (Normal)
                'sample3', // Breast (Tumor)
                'sample1', // Lung (Normal)
                'sample2', // Lung (Tumor)
                203, // Cell count (Normal)
                100 // Cell count (Tumor)
        ]
        // Patient TNS:43
        dataTable.rows[2].cells == [
                 52,
                 'sample9',
                 null,
                 ['sample7', 'sample8'],
                 'sample6',
                 [380, 240],
                 28
        ]
    }

}
