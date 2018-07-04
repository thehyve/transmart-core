/* Copyright © 2017 The Hyve B.V. */
package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec
import com.fasterxml.jackson.databind.ObjectMapper
import com.opencsv.CSVReader
import groovy.transform.CompileStatic
import representations.DataTable
import representations.ExportJob

import java.util.stream.Collectors
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import static base.ContentTypeFor.JSON
import static base.ContentTypeFor.ZIP
import static config.Config.*

class DataTableSpec extends RESTSpec {

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

    final static char COLUMN_SEPARATOR = '\t' as char

    @CompileStatic
    private static List<List<String>> readExportData(byte[] bytes) {
        def zipInputStream
        try {
            zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))
            ZipEntry entry
            while (entry = zipInputStream.nextEntry) {
                if (entry.name == 'data.tsv') {
                    def reader = new BufferedReader(new InputStreamReader(zipInputStream))
                    def csvReader = new CSVReader(reader, COLUMN_SEPARATOR)
                    return csvReader.readAll().stream()
                            .map({ String[] line -> Arrays.asList(line) })
                            .collect(Collectors.toList())
                }
            }
        } finally {
            if (zipInputStream) zipInputStream.close()
        }
        throw new RuntimeException('No export data found.')
    }

    /**
     *  given: study TUMOR_NORMAL_SAMPLES is loaded
     *  when: for that study I export all observations in table format
     *  then: the export contains the expected data
     */
    @RequiresStudy(TUMOR_NORMAL_SAMPLES_ID)
    def 'export data table with a modifier dimension'() {
        given: "study TUMOR_NORMAL_SAMPLES is loaded"
        def exportParams = [
                constraint: [type: 'and', args: [
                        [type: 'subselection', dimension: 'patient',
                                constraint: [type: 'study_name', studyId: 'TUMOR_NORMAL_SAMPLES']],
                        [type: 'or', args: [
                                [type: 'and', args: [[type: "concept", conceptCode: "TNS:DEM:AGE"], [type: "study_name", studyId: "TUMOR_NORMAL_SAMPLES"]]],
                                [type: 'and', args: [[type: "concept", conceptCode: "TNS:HD:EXPBREAST"], [type: "study_name", studyId: "TUMOR_NORMAL_SAMPLES"]]],
                                [type: 'and', args: [[type: "concept", conceptCode: "TNS:HD:EXPLUNG"], [type: "study_name", studyId: "TUMOR_NORMAL_SAMPLES"]]],
                                [type: 'and', args: [[type: "concept", conceptCode: "TNS:LAB:CELLCNT"], [type: "study_name", studyId: "TUMOR_NORMAL_SAMPLES"]]]
                        ]]
                ]],
                elements: [
                        [dataType: 'clinical',
                         format: 'TSV',
                         dataView: 'dataTable'
                        ]
                ],
                includeMeasurementDateColumns: true,
                tableConfig: [
                        rowDimensions: ["patient"],
                        columnDimensions: ["study","concept","sample_type"],
                        rowSort: [],
                        columnSort: []
                ]
        ]

        when: 'I create an export job'
        def createJobRequest = [
                path: "${PATH_DATA_EXPORT}/job",
                acceptType: JSON,
                user: ADMIN_USER
        ]
        def response = post(createJobRequest)
        def jobData = ExportJob.from(response.exportJob)
        def jobId = jobData.id
        def jobName = jobData.jobName

        def exportRequest = [
                path: "${PATH_DATA_EXPORT}/${jobId}/run",
                body: exportParams,
                acceptType: JSON,
                user: ADMIN_USER
        ]

        response = post(exportRequest)
        jobData = ExportJob.from(response.exportJob)

        then: "job has been started"
        assert jobData != null

        assert jobData.id == jobId
        assert jobData.userId == getUsername(ADMIN_USER)
        assert jobData.jobStatus == 'Started'

        when: "checking the status of the job"
        int maxAttemptNumber = 10 // max number of status check attempts
        def statusRequest = [
                path: "$PATH_DATA_EXPORT/$jobId/status",
                acceptType: JSON,
                user: ADMIN_USER
        ]
        response = get(statusRequest)
        jobData = ExportJob.from(response.exportJob)

        then: 'eventually status is Completed'
        assert jobData != null
        def status = jobData.jobStatus

        // waiting for async process to end (increase number of attempts if needed)
        for (int attemptNum = 0; status != 'Completed' && attemptNum < maxAttemptNumber; attemptNum++) {
            sleep(500)
            response = get(statusRequest)
            jobData = ExportJob.from(response.exportJob)
            status = jobData.jobStatus
        }
        assert status == 'Completed'

        when: 'I download the exported file'
        def downloadRequest = [
                path: "${PATH_DATA_EXPORT}/${jobId}/download",
                acceptType: ZIP,
                user: ADMIN_USER
        ]
        byte[] downloadResponse = get(downloadRequest)
        def exportData = readExportData(downloadResponse)
        println "Export data: ${exportData}"

        then: 'it contains the correct data'
        exportData != null
        exportData.size() == 3 + 3
        exportData.each { row ->
            assert row.size() == 8
        }
        exportData[0] == ['', 'TUMOR_NORMAL_SAMPLES', 'TUMOR_NORMAL_SAMPLES', 'TUMOR_NORMAL_SAMPLES', 'TUMOR_NORMAL_SAMPLES', 'TUMOR_NORMAL_SAMPLES', 'TUMOR_NORMAL_SAMPLES', 'TUMOR_NORMAL_SAMPLES']
        exportData[1] == ['', 'TNS:DEM:AGE', 'TNS:HD:EXPBREAST', 'TNS:HD:EXPBREAST', 'TNS:HD:EXPLUNG', 'TNS:HD:EXPLUNG', 'TNS:LAB:CELLCNT', 'TNS:LAB:CELLCNT']
        exportData[2] == ['', '', 'Normal', 'Tumor', 'Normal', 'Tumor', 'Normal', 'Tumor']
        // Patient TNS:63
        exportData[3] == [
                '-63/TNS:63',
                '40.00000',
                '',
                'sample3',
                'sample1',
                'sample2',
                '203.00000',
                '100.00000'
        ]
        // Patient TNS:43
        exportData[5] == [
                '-43/TNS:43',
                '52.00000',
                'sample9',
                '',
                'sample7;sample8',
                'sample6',
                '380.00000;240.00000',
                '28.00000'
        ]

    }

}
