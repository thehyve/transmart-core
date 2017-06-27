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

    static final jobType = "DataExport"

    def "create a new dataExport job"() {
        def name = null
        setUser(DEFAULT_USERNAME, DEFAULT_PASSWORD)
        def request = [
                path      : "$PATH_DATA_EXPORT/job",
                acceptType: contentTypeForJSON,
        ]

        when: "Export job name is NOT specified"
        def responseData = post(request)

        then: "A new job with default name is returned"
        def id = responseData.id
        assert responseData.jobName == "$DEFAULT_USERNAME-$jobType-$id"
        assert responseData.jobStatus == "Started"
        assert responseData.jobStatusTime != null
        assert responseData.jobType == jobType
        assert responseData.viewerURL == null

        when: "Export job name is specified"
        name = 'test_job_name' + id
        request.path = "$PATH_DATA_EXPORT/job"
        request.query = [jobName: name]
        responseData = post(request)

        then: "A new job with specified name is returned "
        assert responseData.jobName == name
        assert responseData.jobStatus == "Started"
        assert responseData.jobStatusTime != null
        assert responseData.jobType == jobType
        assert responseData.viewerURL == null
    }

    def "run data export without 'Export' permission"() {
        setUser(DEFAULT_USERNAME, DEFAULT_PASSWORD)
        def newJobRequest = [
                path      : "$PATH_DATA_EXPORT/job",
                acceptType: contentTypeForJSON,
        ]
        def newJobResponse = post(newJobRequest)
        def jobName = newJobResponse.jobName

        when: "I run a newly created job asynchronously"
        def responseData = get([
                path : "$PATH_DATA_EXPORT/$jobName/run",
                query: ([
                        setType : 'patient',
                        ids     : 28835,
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

    def "run data export with 'Export' permission"() {
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def newJobRequest = [
                path      : "$PATH_DATA_EXPORT/job",
                acceptType: contentTypeForJSON,
        ]
        def newJobResponse = post(newJobRequest)
        def jobName = newJobResponse.jobName

        when: "I run a newly created job asynchronously"
        def runResponse = get([
                path : "$PATH_DATA_EXPORT/$jobName/run",
                query: ([
                        setType : 'patient',
                        ids     : 28835,
                        elements: toJSON([[
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

        then: "The time at which the export will be run by the scheduler is returned"
        assert runResponse != null
        assert runResponse.jobName == jobName
        assert runResponse.scheduledTime != null

        when: "Check the status of the job"
        int maxAttemptNumber = 10 // max number of status check attempts
        def statusRequest = [
                path      : "$PATH_DATA_EXPORT/$jobName/status",
                acceptType: contentTypeForJSON,
        ]
        def statusResponse = get(statusRequest)

        then: "Returned status is 'Completed'"
        assert statusResponse != null
        def status = statusResponse.jobStatus

        // waiting for async process to end (increase number of attempts if needed)
        for (int attempNum = 0; status != 'Completed' && attempNum < maxAttemptNumber; attempNum++) {
            sleep(500)
            statusResponse = get(statusRequest)
            status = statusResponse.jobStatus
        }

        assert status == 'Completed'

        when: "Try to download the file"
        String fileName = "$TEMP_DIRECTORY/$jobName"+".zip"
        def downloadRequest = [
                path      : "$PATH_DATA_EXPORT/$jobName/download",
                acceptType: contentTypeForZip,
        ]
        def downloadResponse = get(downloadRequest)

        then: "ZipStream is returned"
        assert downloadResponse != null
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
        assert createJobResponse in getJobsResponse
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
        def setType = "patient"
        def createPatientSetResponse = post(request)
        def patientSetId = createPatientSetResponse.id

        when: "I check data_formats for created patient_set"
        def getDataFormatsResponse = get([
                path : "$PATH_DATA_EXPORT/data_formats/$patientSetId",
                acceptType: contentTypeForJSON,
                query     : [setType: setType],
        ])

        then: "I get data formats for both clinical and highDim types"
        assert getDataFormatsResponse != null
        assert getDataFormatsResponse.dataFormats.containsAll(["clinical", "mrna"])
    }

    def "get supported file formats"() {
        def supportedFormat = 'TSV'

        def request = [
                path      : "$PATH_DATA_EXPORT/supported_file_formats",
                acceptType: contentTypeForJSON,
        ]

        when: "I request all supported fields"
        def responseData = get(request)

        then: "I get a list of fields containing $supportedFormat format"
        assert supportedFormat in responseData.supportedFileFormats
    }
}
