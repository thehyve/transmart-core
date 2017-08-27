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
import static tests.rest.constraints.ConceptConstraint
import static tests.rest.constraints.ModifierConstraint
import static tests.rest.constraints.TrueConstraint
import static tests.rest.constraints.ValueConstraint

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
        given: "A patientset for TUMOR_NORMAL_SAMPLES study is created"
        def request = [
                path      : PATH_PATIENT_SET,
                acceptType: JSON,
                query     : [name: 'test_set'],
                body      : toJSON([
                        type  : ModifierConstraint, path: "\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\",
                        values: [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: "Tumor"]
                ]),
                statusCode: 201
        ]
        def typeOfSet = "patient"
        def createPatientSetResponse = post(request)
        def patientSetId = createPatientSetResponse.id

        when: "I check data_formats for created patient_set"
        def getDataFormatsResponse = get([
                path      : "$PATH_DATA_EXPORT/data_formats",
                acceptType: JSON,
                query     : [
                        id       : patientSetId,
                        typeOfSet: typeOfSet
                ],
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
                user: DEFAULT_USER
        ]
        def newJobResponse = post(newJobRequest)
        def jobId = newJobResponse.exportJob.id

        when: "I run a newly created job asynchronously"
        def responseData = post([
                path      : "$PATH_DATA_EXPORT/$jobId/run",
                query     : ([
                        typeOfSet: 'patient',
                        id       : patientSetId,
                        elements :
                                toJSON([[
                                                dataType: 'clinical',
                                                format  : 'TSV'
                                        ],
                                        [
                                                dataType: 'mrna',
                                                format  : 'TSV'
                                        ]]),
                ]),
                statusCode: 403,
                user: DEFAULT_USER
        ])
        then: "I get an access error"
        assert responseData.httpStatus == 403
        assert responseData.type == 'AccessDeniedException'
    }

    @RequiresStudy(TUMOR_NORMAL_SAMPLES_ID)
    def "run data export with 'Export' permission"() {
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
                user      : ADMIN_USER
        ]
        def newJobResponse = post(newJobRequest)
        def jobId = newJobResponse.exportJob.id
        def jobName = newJobResponse.exportJob.jobName

        when: "I run a newly created job asynchronously"
        def runResponse = post([
                path      : "$PATH_DATA_EXPORT/$jobId/run",
                query     : ([
                        typeOfSet: 'patient',
                        id       : patientSetId,
                        elements : toJSON([[
                                                   dataType: 'clinical',
                                                   format  : 'TSV'
                                           ],
                                           [
                                                   dataType: 'mrna',
                                                   format  : 'TSV'
                                           ]]),
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

        cleanup: "Remove created file"
        if (fileName) { new File(fileName).delete() }
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
        def supportedFormat = 'TSV'

        def request = [
                path      : "$PATH_DATA_EXPORT/file_formats",
                acceptType: JSON,
        ]

        when: "I request all supported fields"
        def responseData = get(request)

        then:
        "I get a list of fields containing $supportedFormat format"
        assert supportedFormat in responseData.fileFormats
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
        def runResponse1 = post([
                path      : "$PATH_DATA_EXPORT/$jobId/run",
                query     : ([
                        typeOfSet : 'patient',
                        elements  : toJSON([[
                                                    dataType: 'clinical',
                                                    format  : 'TSV'
                                            ]]),
                ]),
                acceptType: JSON,
                statusCode: 400,
        ])
        then: "I get the error."
        runResponse1.message == 'Whether id or constraint parameters can be supplied.'

        when: "I run a newly created job with both id and constraint parameter."
        def runResponse2 = post([
                path      : "$PATH_DATA_EXPORT/$jobId/run",
                query     : ([
                        typeOfSet : 'patient',
                        id : -1,
                        constraint: toJSON([type: TrueConstraint]),
                        elements  : toJSON([[
                                                    dataType: 'clinical',
                                                    format  : 'TSV'
                                            ]]),
                ]),
                acceptType: JSON,
                statusCode: 400
        ])
        then: "I get the error."
        runResponse2.message == 'Whether id or constraint parameters can be supplied.'
    }

    @RequiresStudy(EHR_ID)
    def "run data export using a constraint"() {
        def newJobRequest = [
                path      : "$PATH_DATA_EXPORT/job",
                acceptType: JSON,
        ]
        def newJobResponse = post(newJobRequest)
        def jobId = newJobResponse.exportJob.id
        def jobName = newJobResponse.exportJob.jobName

        when: "I run a newly created job asynchronously"
        def runResponse = post([
                path      : "$PATH_DATA_EXPORT/$jobId/run",
                query     : ([
                        typeOfSet: 'patient',
                        constraint: toJSON([type: ConceptConstraint,
                                           path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"]),
                        elements : toJSON([[
                                                   dataType: 'clinical',
                                                   format  : 'TSV'
                                           ]]),
                ]),
                acceptType: JSON,
        ])

        then: "Job instance with status: 'Started' is returned"
        assert runResponse != null
        assert runResponse.exportJob.id == jobId
        assert runResponse.exportJob.jobStatus == 'Started'
        assert runResponse.exportJob.jobStatusTime != null
        assert runResponse.exportJob.userId == DEFAULT_USER
        assert runResponse.viewerUrl == null

        when: "Check the status of the job"
        String fileName = "$TEMP_DIRECTORY/$DEFAULT_USER/$jobName" + ".zip"
        int maxAttemptNumber = 10 // max number of status check attempts
        def statusRequest = [
                path      : "$PATH_DATA_EXPORT/$jobId/status",
                acceptType: JSON,
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
        ]
        def downloadResponse = get(downloadRequest)

        then: "ZipStream is returned"
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
        assert new File(fileName).isFile()

        cleanup: "Remove created file"
        if (fileName) { new File(fileName).delete() }
    }

    @RequiresStudy(EHR_ID)
    def "export wide file format"() {
        def newJobRequest = [
                path      : "$PATH_DATA_EXPORT/job",
                acceptType: JSON,
        ]
        def newJobResponse = post(newJobRequest)
        def jobId = newJobResponse.exportJob.id
        def jobName = newJobResponse.exportJob.jobName

        when: "I run a newly created job asynchronously"
        def runResponse = post([
                path      : "$PATH_DATA_EXPORT/$jobId/run",
                query     : ([
                        typeOfSet: 'patient',
                        constraint: toJSON([type: ConceptConstraint,
                                            path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"]),
                        elements : toJSON([[
                                                   dataType: 'clinical',
                                                   format  : 'TSV',
                                                    wide   : true
                                           ]]),
                ]),
                acceptType: JSON,
        ])

        then: "Job instance with status: 'Started' is returned"
        assert runResponse != null
        assert runResponse.exportJob.id == jobId
        assert runResponse.exportJob.jobStatus == 'Started'
        assert runResponse.exportJob.jobStatusTime != null
        assert runResponse.exportJob.userId == DEFAULT_USER
        assert runResponse.viewerUrl == null

        when: "Check the status of the job"
        String fileName = "$TEMP_DIRECTORY/$DEFAULT_USER/$jobName" + ".zip"
        int maxAttemptNumber = 10 // max number of status check attempts
        def statusRequest = [
                path      : "$PATH_DATA_EXPORT/$jobId/status",
                acceptType: JSON,
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
        ]
        def downloadResponse = get(downloadRequest)

        then: "ZipStream is returned"
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
        assert new File(fileName).isFile()

        cleanup: "Remove created file"
        if (fileName) { new File(fileName).delete() }
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
