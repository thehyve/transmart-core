package org.transmartproject.rest

import com.recomdata.transmart.domain.i2b2.AsyncJob
import grails.transaction.Transactional
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.db.user.User
import org.transmartproject.rest.dataExport.JobStatus
import org.transmartproject.rest.dataExport.RestDataExportService
import org.transmartproject.rest.dataExport.ExportAsyncJobService
import org.transmartproject.rest.dataExport.ExportJobExecutor

import javax.transaction.NotSupportedException

@Transactional
class RestExportService {

    Scheduler quartzScheduler
    @Autowired
    RestDataExportService restDataExportService
    @Autowired
    ExportAsyncJobService exportAsyncJobService

    static enum FileFormat {
        TSV('TSV'), // TODO add support for other file formats

        final String value

        FileFormat(String value) { this.value = value }

        String toString() { value }

        String getKey() { name() }
    }

    static enum SupportedSetTypes {
        OBSERVATION('observation'),
        PATIENT('patient')

        final String value

        SupportedSetTypes(String value) { this.value = value }

        String toString() { value }

        String getKey() { name() }
    }

    static supportedFileFormats = FileFormat.values().collect { it.toString() }
    static supportedSetTypes = SupportedSetTypes.values().collect { it.toString() }

    static final clinicalDataType = "clinical"

    AsyncJob createExportJob(User user, String name) {
        AsyncJob newJob = exportAsyncJobService.createNewJob(user, name)
        log.debug("Sending ${newJob.jobName} back to the client")
        newJob
    }

    def exportData(List<Long> ids, String setType, List types, User user, String jobName) {
        def job = exportAsyncJobService.getJobByName(jobName)
        if (job.jobStatus != JobStatus.STARTED.value) {
            throw new InvalidRequestException("Job with a name '$jobName' has invalid status. " +
                    "Expected: $JobStatus.STARTED.value, actual: $job.jobStatus")
        }

        def dataMap = createJobDataMap(ids, setType, types, user, jobName)
        executeExportJob(dataMap)
    }

    def downloadFile(String jobName) {
        def job = exportAsyncJobService.getJobByName(jobName)
        if(job.jobStatus != JobStatus.COMPLETED.value) {
            throw new InvalidRequestException("Job with a name is not completed. Current status: '$job.jobStatus'")
        }

        def exportJobExecutor = new ExportJobExecutor()
        return exportJobExecutor.getExportJobFileStream(job.viewerURL)
    }

    def listJobs(User user) {
        exportAsyncJobService.getJobList(user)
    }

    Boolean isJobNameUnique(String name) {
        exportAsyncJobService.getJobByName(name) == null
    }

    def jobStatus(String jobName) {
        exportAsyncJobService.checkJobStatus(jobName)
    }

    def jobUser(String name) {
        exportAsyncJobService.getJobUser(name)
    }

    def isUserAllowedToExport(List<Long> resultSetIds, User user, String setType) {
        if (setType == SupportedSetTypes.PATIENT.value) {
            restDataExportService.patientSetsExportPermission(resultSetIds, user)
        }
    }

    List getDataFormats(String setType, List<Long> setIds, User user) {

        // clinical data (always included)
        List<String> dataFormats = []
        dataFormats.add(clinicalDataType)

        // highDim data
        switch (setType) {
            case SupportedSetTypes.PATIENT.value:
                List highDimDataTypes = restDataExportService.highDimDataTypesForPatientSets(setIds, user)
                if (highDimDataTypes) dataFormats.addAll(highDimDataTypes)
                break
            case SupportedSetTypes.OBSERVATION.value:
                List highDimDataTypes = restDataExportService.highDimDataTypesForObservationSets(setIds, user)
                if (highDimDataTypes) dataFormats.addAll(highDimDataTypes)
                break
            default:
                throw new NotSupportedException("Set type '$setType' not supported.")
        }

        dataFormats
    }

    private def executeExportJob(Map dataMap) {

        def jobDataMap = new JobDataMap(dataMap)
        JobDetail jobDetail = JobBuilder.newJob(ExportJobExecutor.class)
                .withIdentity(dataMap.jobName, 'DataExport')
                .setJobData(jobDataMap)
                .build()

        exportAsyncJobService.updateStatus(dataMap.jobName, JobStatus.TRIGGERING_JOB)

        def randomDelay = Math.random()*10 as int
        def startTime = new Date(new Date().time + randomDelay)
        def trigger = TriggerBuilder.newTrigger()
                .startAt(startTime)
                .withIdentity('DataExport')
                .build()
        quartzScheduler.scheduleJob(jobDetail, trigger)
    }

    private static Map createJobDataMap(List<Long> ids, String setType, List types, User user, String jobName) {
        [
                user                 : user,
                jobName              : jobName,
                ids                  : ids,
                setType              : setType,
                dataTypeAndFormatList: types
        ]
    }
}
