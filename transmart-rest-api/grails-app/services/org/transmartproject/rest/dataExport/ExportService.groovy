package org.transmartproject.rest.dataExport

import grails.transaction.Transactional
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.db.job.AsyncJobCoreDb as AsyncJob
import org.transmartproject.db.user.User

import javax.transaction.NotSupportedException

@Transactional
@Component("restExportService")
class ExportService {

    @Autowired(required = false)
    Scheduler quartzScheduler
    @Autowired
    DataExportService restDataExportService
    @Autowired
    ExportAsyncJobService exportAsyncJobService

    static enum FileFormat {
        TSV('TSV'), // TODO add support for other file formats

        final String value

        FileFormat(String value) { this.value = value }

        String toString() { value }

        String getKey() { name() }
    }

    static enum SupportedTypesOfSet {
        OBSERVATION('observation'),
        PATIENT('patient')

        final String value

        SupportedTypesOfSet(String value) { this.value = value }

        String toString() { value }

        String getKey() { name() }
    }

    static supportedFileFormats = FileFormat.values().collect { it.toString() }
    static supportedTypesOfSet = SupportedTypesOfSet.values().collect { it.toString() }

    static final clinicalDataType = "clinical"

    AsyncJob createExportJob(User user, String name) {
        AsyncJob newJob = exportAsyncJobService.createNewJob(user, name)
        log.debug("Sending ${newJob.jobName} back to the client")
        newJob
    }

    def exportData(List<Long> ids, String typeOfSet, List types, User user, Long jobId) {
        def job = jobById(jobId)
        if (job.jobStatus != JobStatus.CREATED.value) {
            throw new InvalidRequestException("Job with id $jobId has invalid status. " +
                    "Expected: $JobStatus.CREATED.value, actual: $job.jobStatus")
        }

        def dataMap = createJobDataMap(ids, typeOfSet, types, user, jobId, job.jobName)
        executeExportJob(dataMap)
    }

    def downloadFile(Long jobId) {
        def job = jobById(jobId)
        if(job.jobStatus != JobStatus.COMPLETED.value) {
            throw new InvalidRequestException("Job with a name is not completed. Current status: '$job.jobStatus'")
        }

        def exportJobExecutor = new ExportJobExecutor()
        return exportJobExecutor.getExportJobFileStream(job.viewerURL)
    }

    def listJobs(User user) {
        exportAsyncJobService.getJobList(user)
    }

    Boolean isJobNameUniqueForUser(String name, User user) {
        jobByNameAndUser(name, user) == null
    }

    def jobByNameAndUser(String jobName, User user) {
        exportAsyncJobService.getJobByNameAndUser(jobName, user)
    }

    def jobById(Long id) {
        exportAsyncJobService.getJobById(id)
    }

    def jobUser(Long id) {
        exportAsyncJobService.getJobUser(id)
    }

    def isUserAllowedToExport(List<Long> resultSetIds, User user, String typeOfSet) {
        if (typeOfSet == SupportedTypesOfSet.PATIENT.value) {
            restDataExportService.patientSetsExportPermission(resultSetIds, user)
        }
    }

    List getDataFormats(String typeOfSet, List<Long> setIds, User user) {

        // clinical data (always included)
        List<String> dataFormats = []
        dataFormats.add(clinicalDataType)

        // highDim data
        switch (typeOfSet) {
            case SupportedTypesOfSet.PATIENT.value:
                List highDimDataTypes = restDataExportService.highDimDataTypesForPatientSets(setIds, user)
                if (highDimDataTypes) dataFormats.addAll(highDimDataTypes)
                break
            case SupportedTypesOfSet.OBSERVATION.value:
                List highDimDataTypes = restDataExportService.highDimDataTypesForObservationSets(setIds, user)
                if (highDimDataTypes) dataFormats.addAll(highDimDataTypes)
                break
            default:
                throw new NotSupportedException("Set type '$typeOfSet' not supported.")
        }

        dataFormats
    }

    private AsyncJob executeExportJob(Map dataMap) {

        def job
        def jobDataMap = new JobDataMap(dataMap)
        JobDetail jobDetail = JobBuilder.newJob(ExportJobExecutor.class)
                .withIdentity(dataMap.jobId.toString(), 'DataExport')
                .setJobData(jobDataMap)
                .build()
        def randomDelay = Math.random()*10 as int
        def startTime = new Date(new Date().time + randomDelay)
        def trigger = TriggerBuilder.newTrigger()
                .startAt(startTime)
                .withIdentity('DataExport')
                .build()

        if(!quartzScheduler) {
            job = exportAsyncJobService.updateStatus(dataMap.jobId, JobStatus.ERROR, null,
                    "No qualifying bean found for dependency 'org.quartz.Scheduler'")
        } else {
            job = exportAsyncJobService.updateStatus(dataMap.jobId, JobStatus.STARTED)
            quartzScheduler.scheduleJob(jobDetail, trigger)
        }

        return job
    }

    private static Map createJobDataMap(List<Long> ids, String typeOfSet, List types, User user, Long jobId, String jobName) {
        [
                user                 : user,
                jobName              : jobName,
                jobId                : jobId,
                ids                  : ids,
                typeOfSet            : typeOfSet,
                dataTypeAndFormatList: types
        ]
    }
}
