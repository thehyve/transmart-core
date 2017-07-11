package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec
import groovy.util.logging.Slf4j

import static base.ContentTypeFor.contentTypeForZip
import static tests.rest.v2.Operator.EQUALS
import static tests.rest.v2.ValueType.STRING
import static tests.rest.v2.constraints.ModifierConstraint
import static base.ContentTypeFor.contentTypeForJSON
import static config.Config.*
import static tests.rest.v2.constraints.ValueConstraint

@Slf4j
class DataExportSpec extends RESTSpec {

    def "create a new dataExport job"() {
        def name = null
        setUser(DEFAULT_USERNAME, DEFAULT_PASSWORD)
        def request = [
                path      : "$PATH_DATA_EXPORT/job",
                acceptType: contentTypeForJSON,
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
        assert responseData.userId == DEFAULT_USERNAME
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
        assert responseData.userId == DEFAULT_USERNAME
        assert responseData.viewerUrl == null
    }

    @RequiresStudy(TUMOR_NORMAL_SAMPLES_ID)
    def "get data formats for patientSet"() {
        given: "A patientset for TUMOR_NORMAL_SAMPLES study is created"
        def request = [
                path      : PATH_PATIENT_SET,
                acceptType: contentTypeForJSON,
                query     : [name: 'test_set'],
                body      : toJSON([
                        type  : ModifierConstraint, path: "\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\",
                        values: [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: "Tumor"]
                ]),
        ]
        def typeOfSet = "patient"
        def createPatientSetResponse = post(request)
        def patientSetId = createPatientSetResponse.id

        when: "I check data_formats for created patient_set"
        def getDataFormatsResponse = get([
                path : "$PATH_DATA_EXPORT/data_formats",
                acceptType: contentTypeForJSON,
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
        setUser(DEFAULT_USERNAME, DEFAULT_PASSWORD)
        def patientSetRequest = [
                path      : PATH_PATIENT_SET,
                acceptType: contentTypeForJSON,
                query     : [name: 'export_test_set'],
                body      : toJSON([
                        type  : ModifierConstraint, path: "\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\",
                        values: [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: "Tumor"]
                ]),
        ]
        def createPatientSetResponse = post(patientSetRequest)
        def patientSetId = createPatientSetResponse.id

        def newJobRequest = [
                path      : "$PATH_DATA_EXPORT/job",
                acceptType: contentTypeForJSON,
        ]
        def newJobResponse = post(newJobRequest)
        def jobId = newJobResponse.exportJob.id

        when: "I run a newly created job asynchronously"
        def responseData = post([
                path : "$PATH_DATA_EXPORT/$jobId/run",
                query: ([
                        typeOfSet : 'patient',
                        id      : patientSetId,
                        elements:
                                toJSON([[
                                                  dataType: 'clinical',
                                                  format  : 'TSV'
                                          ],
                                          [
                                                  dataType: 'mrna',
                                                  format  : 'TSV'
                                          ]]),
                ]),
                statusCode: 403
        ])
        then: "I get an access error"
        assert responseData.httpStatus == 403
        assert responseData.type == 'AccessDeniedException'
    }

    @RequiresStudy(TUMOR_NORMAL_SAMPLES_ID)
    def "run data export with 'Export' permission"() {
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)

        def patientSetRequest = [
                path      : PATH_PATIENT_SET,
                acceptType: contentTypeForJSON,
                query     : [name: 'export_test_set'],
                body      : toJSON([
                        type  : ModifierConstraint, path: "\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\",
                        values: [type: ValueConstraint, valueType: STRING, operator: EQUALS, value: "Tumor"]
                ]),
        ]
        def createPatientSetResponse = post(patientSetRequest)
        def patientSetId = createPatientSetResponse.id

        def newJobRequest = [
                path      : "$PATH_DATA_EXPORT/job",
                acceptType: contentTypeForJSON,
        ]
        def newJobResponse = post(newJobRequest)
        def jobId = newJobResponse.exportJob.id
        def jobName = newJobResponse.exportJob.jobName

        when: "I run a newly created job asynchronously"
        def runResponse = post([
                path : "$PATH_DATA_EXPORT/$jobId/run",
                query: ([
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
                acceptType: contentTypeForJSON,
        ])

        then: "Job instance with status: 'Started' is returned"
        assert runResponse != null
        assert runResponse.exportJob.id == jobId
        assert runResponse.exportJob.jobStatus == 'Started'
        assert runResponse.exportJob.jobStatusTime != null
        assert runResponse.exportJob.userId == 'admin'
        assert runResponse.viewerUrl == null

        when: "Check the status of the job"
        String fileName = "$TEMP_DIRECTORY/$ADMIN_USERNAME/$jobName"+".zip"
        int maxAttemptNumber = 10 // max number of status check attempts
        def statusRequest = [
                path      : "$PATH_DATA_EXPORT/$jobId/status",
                acceptType: contentTypeForJSON,
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
                acceptType: contentTypeForZip,
        ]
        def downloadResponse = get(downloadRequest)

        then: "ZipStream is returned"
        assert downloadResponse != null
        assert downloadResponse.eofWatcher.wrappedEntity.contentType.value == contentTypeForZip
        assert new File(fileName).isFile()

        cleanup: "Remove created file"
        new File(fileName).delete()
    }

    def "list all dataExport jobs for user"() {
        setUser(DEFAULT_USERNAME, DEFAULT_PASSWORD)
        def createJobRequest = [
                path      : "$PATH_DATA_EXPORT/job",
                acceptType: contentTypeForJSON,
        ]
        def createJobResponse = post(createJobRequest)

        when: "I try to fetch list of all export jobs"
        def getJobsResponse = get([
                path : "$PATH_DATA_EXPORT/jobs",
                acceptType: contentTypeForJSON,
        ])

        then: "The list of all data export job, including the newly created one is returned"
        assert getJobsResponse != null
        assert createJobResponse.exportJob in getJobsResponse.exportJobs
    }

    def "get supported file formats"() {
        def supportedFormat = 'TSV'

        def request = [
                path      : "$PATH_DATA_EXPORT/file_formats",
                acceptType: contentTypeForJSON,
        ]

        when: "I request all supported fields"
        def responseData = get(request)

        then: "I get a list of fields containing $supportedFormat format"
        assert supportedFormat in responseData.fileFormats
    }
}
