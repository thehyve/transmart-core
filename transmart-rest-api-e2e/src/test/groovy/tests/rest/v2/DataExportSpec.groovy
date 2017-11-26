package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec
import groovy.util.logging.Slf4j

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import static base.ContentTypeFor.JSON
import static base.ContentTypeFor.ZIP
import static config.Config.*
import static tests.rest.Operator.EQUALS
import static tests.rest.ValueType.STRING
import static tests.rest.constraints.*

@Slf4j
class DataExportSpec extends RESTSpec {

    def "create a new dataExport job"() {
        def name = null
        def request = [
                path      : "$PATH_DATA_EXPORT/job",
                acceptType: JSON,
        ]

        when: "Export job name is NOT specified"
        def response = post(request)
        def responseData = response.exportJob
        def id = responseData.id

        then: "A new job with default name is returned"
        assert id != null
        assert responseData.jobName == id.toString()
        assert responseData.jobStatus == "Created"
        assert responseData.jobStatusTime != null
        assert responseData.userId == DEFAULT_USER
        assert responseData.viewerUrl == null

        when: "Export job name is specified"
        name = 'test_job_name' + id
        request.path = "$PATH_DATA_EXPORT/job"
        request.query = [name: name]
        response = post(request)
        responseData = response.exportJob

        then: "A new job with specified name is returned "
        assert responseData.id != null
        assert responseData.jobName == name
        assert responseData.jobStatus == "Created"
        assert responseData.jobStatusTime != null
        assert responseData.userId == DEFAULT_USER
        assert responseData.viewerUrl == null
    }

    @RequiresStudy(TUMOR_NORMAL_SAMPLES_ID)
    def "get data formats for patientSet"() {
        when: "I check data_formats for the constraint"
        def getDataFormatsResponse = post([
                path      : "$PATH_DATA_EXPORT/data_formats",
                acceptType: JSON,
                body      : toJSON([
                        constraint: [
                                type  : ModifierConstraint, path: "\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\",
                                values: [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: "Tumor"]
                        ],
                ]),
        ])

        then: "I get data formats for both clinical and highDim types"
        assert getDataFormatsResponse != null
        assert getDataFormatsResponse.dataFormats.containsAll(["clinical", "mrna"])
    }

    @RequiresStudy(TUMOR_NORMAL_SAMPLES_ID)
    def "run data export without 'Export' permission"() {
        def patientSetRequest = [
                path      : PATH_PATIENT_SET,
                acceptType: JSON,
                query     : [name: 'export_test_set'],
                body      : toJSON([
                        type  : ModifierConstraint, path: "\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\",
                        values: [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: "Tumor"]
                ]),
                user      : ADMIN_USER,
                statusCode: 201
        ]
        def createPatientSetResponse = post(patientSetRequest)
        def patientSetId = createPatientSetResponse.id

        def newJobRequest = [
                path      : "$PATH_DATA_EXPORT/job",
                acceptType: JSON,
                user      : DEFAULT_USER
        ]
        def newJobResponse = post(newJobRequest)
        def jobId = newJobResponse.exportJob.id

        when: "I run a newly created job asynchronously"
        def responseData = post([
                path      : "$PATH_DATA_EXPORT/$jobId/run",
                body      : toJSON([
                        constraint: [type: PatientSetConstraint, patientSetId: patientSetId],
                        elements  :
                                [[
                                         dataType: 'clinical',
                                         format  : 'TSV'
                                 ],
                                 [
                                         dataType: 'mrna',
                                         format  : 'TSV'
                                 ]],
                ]),
                user      : DEFAULT_USER
        ])
        then: "Job instance with status: 'Started' is returned"
        responseData != null
        responseData.exportJob.id == jobId
        responseData.exportJob.jobStatus == 'Started'
        responseData.exportJob.jobStatusTime != null
        responseData.exportJob.userId == DEFAULT_USER
        responseData.viewerUrl == null

        when: "Check the status of the job"
        int maxAttemptNumber = 10 // max number of status check attempts
        def statusRequest = [
                path      : "$PATH_DATA_EXPORT/$jobId/status",
                acceptType: JSON,
                user      : DEFAULT_USER
        ]
        def statusResponse = get(statusRequest)

        then: "Returned status is 'Error'"
        statusResponse != null
        def status = statusResponse.exportJob.jobStatus

        // waiting for async process to end (increase number of attempts if needed)
        for (int attempNum = 0; status != 'Error' && status != 'Completed' && attempNum < maxAttemptNumber; attempNum++) {
            sleep(500)
            statusResponse = get(statusRequest)
            status = statusResponse.exportJob.jobStatus
        }

        status == 'Error'
        statusResponse.exportJob.message == "Access denied to patient set or patient set does not exist: ${patientSetId}"
    }

    @RequiresStudy(TUMOR_NORMAL_SAMPLES_ID)
    def "run data export"() {
        def newJobRequest = [
                path      : "$PATH_DATA_EXPORT/job",
                acceptType: JSON,
                user      : ADMIN_USER
        ]
        def newJobResponse = post(newJobRequest)
        def jobId = newJobResponse.exportJob.id
        def jobName = newJobResponse.exportJob.jobName

        when: "I run a newly created job asynchronously"
        def runResponse = post([
                path      : "$PATH_DATA_EXPORT/$jobId/run",
                body      : toJSON([
                        constraint: [
                                type  : ModifierConstraint, path: "\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\",
                                values: [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: "Tumor"]
                        ],
                        elements  : [[
                                             dataType: 'clinical',
                                             format  : 'TSV'
                                     ],
                                     [
                                             dataType: 'mrna',
                                             format  : 'TSV'
                                     ]],
                ]),
                acceptType: JSON,
                user      : ADMIN_USER
        ])

        then: "Job instance with status: 'Started' is returned"
        assert runResponse != null
        assert runResponse.exportJob.id == jobId
        assert runResponse.exportJob.jobStatus == 'Started'
        assert runResponse.exportJob.jobStatusTime != null
        assert runResponse.exportJob.userId == 'admin'
        assert runResponse.viewerUrl == null

        when: "Check the status of the job"
        String fileName = "$TEMP_DIRECTORY/$ADMIN_USER/$jobName" + ".zip"
        int maxAttemptNumber = 10 // max number of status check attempts
        def statusRequest = [
                path      : "$PATH_DATA_EXPORT/$jobId/status",
                acceptType: JSON,
                user      : ADMIN_USER
        ]
        def statusResponse = get(statusRequest)

        then: "Returned status is 'Completed'"
        assert statusResponse != null
        def status = statusResponse.exportJob.jobStatus

        // waiting for async process to end (increase number of attempts if needed)
        for (int attempNum = 0; status != 'Completed' && attempNum < maxAttemptNumber; attempNum++) {
            sleep(500)
            statusResponse = get(statusRequest)
            status = statusResponse.exportJob.jobStatus
        }

        assert status == 'Completed'
        assert statusResponse.exportJob.viewerUrl == fileName

        when: "Try to download the file"
        def downloadRequest = [
                path      : "$PATH_DATA_EXPORT/$jobId/download",
                acceptType: ZIP,
                user      : ADMIN_USER
        ]
        def downloadResponse = get(downloadRequest)

        then: "ZipStream is returned"
        assert downloadResponse != null
        assert new File(fileName).isFile()

        when: "Try to download the file"
        def downloadRequest2 = [
                path      : "$PATH_DATA_EXPORT/$jobId/download",
                acceptType: ZIP,
                user      : DEFAULT_USER,
                statusCode: 403
        ]
        def downloadResponse2 = get(downloadRequest2)

        then: "error is returned"
        downloadResponse2.message == "Job ${jobId} was not created by this user"

        cleanup: "Remove created file"
        if (fileName) {
            new File(fileName).delete()
        }
    }

    def "list all dataExport jobs for user"() {
        def createJobRequest = [
                path      : "$PATH_DATA_EXPORT/job",
                acceptType: JSON,
        ]
        def createJobResponse = post(createJobRequest)

        when: "I try to fetch list of all export jobs"
        def getJobsResponse = get([
                path      : "$PATH_DATA_EXPORT/jobs",
                acceptType: JSON,
        ])

        then: "The list of all data export job, including the newly created one is returned"
        assert getJobsResponse != null
        assert createJobResponse.exportJob in getJobsResponse.exportJobs
    }

    def "get supported file formats"() {
        def request = [
                path      : "$PATH_DATA_EXPORT/file_formats",
                acceptType: JSON,
        ]

        when: "I request all supported fields"
        def responseData = get(request)

        then:
        "I get a list of fields containing the supported formats"
        'TSV' in responseData.fileFormats
        'PROTOBUF' in responseData.fileFormats
        'JSON' in responseData.fileFormats
    }

    @RequiresStudy(TUMOR_NORMAL_SAMPLES_ID)
    def "run data export with either id or constraint parameter only"() {
        def newJobRequest = [
                path      : "$PATH_DATA_EXPORT/job",
                acceptType: JSON,
        ]
        def newJobResponse = post(newJobRequest)
        def jobId = newJobResponse.exportJob.id

        when: "I run a newly created job without id nor constraint parameter supplied."
        def runResponse = post([
                path      : "$PATH_DATA_EXPORT/$jobId/run",
                body      : toJSON([
                        elements: [[
                                           dataType: 'clinical',
                                           format  : 'TSV'
                                   ]],
                ]),
                acceptType: JSON,
                statusCode: 400,
        ])
        then: "I get the error."
        runResponse.message == 'No constraint provided.'
    }

    @RequiresStudy(EHR_ID)
    def "run data export using a constraint"() {
        when:
        def downloadResponse = runTypicalExport([
                constraint: [type: ConceptConstraint,
                             path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"],
                elements  : [[
                                     dataType: 'clinical',
                                     format  : 'TSV'
                             ]]
        ])
        then:
        assert downloadResponse != null
        def filesLineNumbers = getFilesLineNumbers(downloadResponse as byte[])
        filesLineNumbers['clinical_observations.tsv'] == 10
        filesLineNumbers['clinical_study.tsv'] == 2
        filesLineNumbers['clinical_concept.tsv'] == 2
        filesLineNumbers['clinical_patient.tsv'] == 4
        filesLineNumbers['clinical_visit.tsv'] == 8
        filesLineNumbers['clinical_trial_visit.tsv'] == 2
        filesLineNumbers['clinical_provider.tsv'] == 1
        filesLineNumbers['clinical_sample_type.tsv'] == 1
    }

    @RequiresStudy(SURVEY1_ID)
    def "export survey to tsv file format"() {
        when:
        def downloadResponse = runTypicalExport([
                constraint: [type   : StudyNameConstraint,
                             studyId: SURVEY1_ID],
                elements  : [[
                                     dataType: 'clinical',
                                     format  : 'TSV',
                                     dataView : 'surveyTable'
                             ]],
        ])
        then:
        assert downloadResponse != null
        def filesLineNumbers = getFilesLineNumbers(downloadResponse as byte[])
        filesLineNumbers.size() == 3
        filesLineNumbers['data.tsv'] == 15
        filesLineNumbers['variables.tsv'] == 14
        filesLineNumbers['value_labels.tsv'] == 4

    }

    @RequiresStudy(SURVEY1_ID)
    def "export survey to spss file format"() {
        when:
        def downloadResponse = runTypicalExport([
                constraint: [type   : StudyNameConstraint,
                             studyId: SURVEY1_ID],
                elements  : [[
                                     dataType: 'clinical',
                                     format  : 'SPSS',
                                     dataView : 'surveyTable'
                             ]],
        ])
        then:
        assert downloadResponse != null
        def filesLineNumbers = getFilesLineNumbers(downloadResponse as byte[])
        filesLineNumbers.size() == 2
        filesLineNumbers['spss/data.tsv'] == 15
        filesLineNumbers['spss/data.sps'] == 65
    }

    @RequiresStudy(CATEGORICAL_VALUES_ID)
    def "export non conventional study to tsv file format"() {
        when:
        def downloadResponse = runTypicalExport([
                constraint: [type   : StudyNameConstraint,
                             studyId: CATEGORICAL_VALUES_ID],
                elements  : [[
                                     dataType: 'clinical',
                                     format  : 'TSV',
                                     dataView : 'surveyTable'
                             ]],
        ])
        then:
        assert downloadResponse != null
        def filesLineNumbers = getFilesLineNumbers(downloadResponse as byte[])
        filesLineNumbers.size() == 2
        filesLineNumbers['data.tsv'] == 4
        filesLineNumbers['variables.tsv'] == 10
    }

    @RequiresStudy(CATEGORICAL_VALUES_ID)
    def "export not conventional to spss file format"() {
        when:
        def downloadResponse = runTypicalExport([
                constraint: [type   : StudyNameConstraint,
                             studyId: CATEGORICAL_VALUES_ID],
                elements  : [[
                                     dataType: 'clinical',
                                     format  : 'SPSS',
                                     dataView : 'surveyTable'
                             ]],
        ])
        then:
        assert downloadResponse != null
        def filesLineNumbers = getFilesLineNumbers(downloadResponse as byte[])
        filesLineNumbers.size() == 2
        filesLineNumbers['spss/data.tsv'] == 4
        filesLineNumbers['spss/data.sps'] == 44
    }

    def "get supported file formats for survey table"() {
        def request = [
                path      : "$PATH_DATA_EXPORT/file_formats",
                acceptType: JSON,
                query     : [dataView : 'surveyTable']
        ]

        when: "I request all supported fields"
        def responseData = get(request)

        then:
        "I get a list of fields containing the supported formats"
        assert 'TSV' in responseData.fileFormats
        assert 'SPSS' in responseData.fileFormats
    }

    def runTypicalExport(body) {
        def newJobRequest = [
                path      : "$PATH_DATA_EXPORT/job",
                acceptType: JSON,
        ]
        def newJobResponse = post(newJobRequest)
        def jobId = newJobResponse.exportJob.id
        def jobName = newJobResponse.exportJob.jobName

        def runResponse = post([
                path      : "$PATH_DATA_EXPORT/$jobId/run",
                body      : toJSON(body),
                acceptType: JSON,
        ])

        assert runResponse != null
        assert runResponse.exportJob.id == jobId
        assert runResponse.exportJob.jobStatus == 'Started'
        assert runResponse.exportJob.jobStatusTime != null
        assert runResponse.exportJob.userId == DEFAULT_USER
        assert runResponse.viewerUrl == null

        String fileName = "$TEMP_DIRECTORY/$DEFAULT_USER/$jobName" + ".zip"
        int maxAttemptNumber = 10 // max number of status check attempts
        def statusRequest = [
                path      : "$PATH_DATA_EXPORT/$jobId/status",
                acceptType: JSON,
        ]
        def statusResponse = get(statusRequest)

        assert statusResponse != null
        def status = statusResponse.exportJob.jobStatus

        // waiting for async process to end (increase number of attempts if needed)
        for (int attempNum = 0; status != 'Completed' && attempNum < maxAttemptNumber; attempNum++) {
            sleep(500)
            statusResponse = get(statusRequest)
            status = statusResponse.exportJob.jobStatus
        }

        assert status == 'Completed'
        assert statusResponse.exportJob.viewerUrl == fileName

        def downloadRequest = [
                path      : "$PATH_DATA_EXPORT/$jobId/download",
                acceptType: ZIP,
        ]
        def downloadResponse = get(downloadRequest)

        assert new File(fileName).isFile()

        if (fileName) {
            new File(fileName).delete()
        }

        return downloadResponse
    }

    private Map<String, Integer> getFilesLineNumbers(byte[] content) {
        Map<String, Integer> result = [:]
        def zipInputStream
        try {
            zipInputStream = new ZipInputStream(new ByteArrayInputStream(content))
            ZipEntry entry
            while (entry = zipInputStream.nextEntry) {
                def reader = new BufferedReader(new InputStreamReader(zipInputStream))
                Integer linesNumber = 0
                while (reader.readLine()) {
                    linesNumber += 1
                }
                result[entry.name] = linesNumber
            }
        } finally {
            if (zipInputStream) zipInputStream.close()
        }

        result
    }
}
