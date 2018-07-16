package org.transmartproject.rest.dataExport

import grails.transaction.Transactional
import org.quartz.*
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User
import org.transmartproject.db.clinical.AbstractDataResourceService
import org.transmartproject.db.job.AsyncJobCoreDb
import org.transmartproject.db.multidimquery.query.InvalidQueryException
import org.transmartproject.rest.serialization.ExportJobRepresentation

@Transactional
class ExportAsyncJobService extends AbstractDataResourceService {

    Scheduler quartzScheduler

    static String jobType = 'DataExport'

    AsyncJobCoreDb getJobById(Long id) {
        def job = AsyncJobCoreDb.findById(id)
        if (!job) {
            throw new NoSuchResourceException("Job with id '$id' does not exist.")
        }
        return job
    }

    List<AsyncJobCoreDb> getJobList(User user) {
        // TODO add time constraints for retrived jobs
        if(user.admin) {
            AsyncJobCoreDb.findAllByJobType(jobType)
        } else {
            AsyncJobCoreDb.findAllByUserIdAndJobType(user.username, jobType)
        }
    }

    Boolean isJobNameUniqueForUser(String name, User user) {
        getJobByNameAndUser(name, user) == null
    }

    AsyncJobCoreDb getJobByNameAndUser(String jobName, User user) {
        def jobs = AsyncJobCoreDb.findAllByJobNameAndUserId(jobName, user.username)
        if(jobs.size() > 1) {
            throw new InvalidQueryException("More than one job with a name '$jobName' found")
        }
        jobs[0]
    }

    AsyncJobCoreDb createNewJob(User user, String jobName = null) {

        def jobStatus = JobStatus.CREATED

        def newJob = new AsyncJobCoreDb(lastRunOn: new Date())
        newJob.jobType = jobType
        newJob.jobStatus = jobStatus.value
        newJob.jobName = jobName
        newJob.userId = user.username
        newJob.save()

        if (!jobName?.trim()) {
            newJob.jobName = newJob.id.toString()
            newJob.save()
        }

        log.debug("Sending ${newJob.jobName} back to the client")

        newJob
    }
    
    /**
     * Cancels a job
     */
    void cancelJob(Long jobId) {
        log.info("Canceling job with id ${jobId}.")
        AsyncJobCoreDb job = getJobById(jobId)
        if (job.jobStatus == JobStatus.CANCELLED.value) {
            log.info("Job with ${jobId} id has been cancelled already.")
            return
        }
        if (job.inTerminalStatus) {
            throw new IllegalStateException("Can't cancel job with ${jobId} id as it has '${job.jobStatus}' status.")
        }

        JobKey jobKey = getJobKeyForId(jobId)
        if (quartzScheduler.deleteJob(jobKey)) {
            log.info("${jobKey} job was unscheduled.")
        }
        if (quartzScheduler.interrupt(jobKey)) {
            log.info("${jobKey} job has been interrupted.")
        }
        updateStatus(jobId, JobStatus.CANCELLED)
        log.info("${jobKey} job was marked as cancelled.")
    }

    /**
     * Deletes a job after the cancelling it
     */
    void deleteJob(Long jobId) {
        AsyncJobCoreDb job = getJobById(jobId)
        if (!job.inTerminalStatus) {
            log.info("Job with id ${jobId} hasn't finished. Try to cancel it first.")
            try {
                cancelJob(jobId)
            } catch (Exception e) {
                log.error("Can't cancel job with id ${jobId}.", e)
            }
        }
        log.info("Removing job with id ${jobId} from the table.")
        job.delete()
    }

    String getJobUser(Long id) {
        def asyncJobCoreDb = getJobById(id)
        asyncJobCoreDb?.userId
    }

    AsyncJobCoreDb updateStatus(Long id, JobStatus status, String viewerURL = null, String results = null) {
        def job = getJobById(id)
        if (job.jobStatus == JobStatus.CANCELLED.value) {
            log.warn("Job ${job.jobName} has been cancelled")
            return job
        }

        job.jobStatus = status.value
        if (viewerURL?.trim()) job.viewerURL = viewerURL
        if (results?.trim()) job.results = results

        job.save(flush: true)
        return job
    }

    private static JobKey getJobKeyForId(Long jobId) {
        new JobKey(jobId.toString(), 'DataExport')
    }

    private AsyncJobCoreDb executeExportJob(Map dataMap) {

        def job
        def jobDataMap = new JobDataMap(dataMap)
        JobDetail jobDetail = JobBuilder.newJob(ExportJobExecutor.class)
                .withIdentity(getJobKeyForId(dataMap.jobId))
                .setJobData(jobDataMap)
                .build()
        def randomDelay = Math.random()*10 as int
        def startTime = new Date(new Date().time + randomDelay)

        def trigger = TriggerBuilder.newTrigger()
                .startAt(startTime)
                .withIdentity("${dataMap.jobId.toString()}-Trigger", 'DataExport')
                .build()

        if(!quartzScheduler) {
            job = updateStatus(dataMap.jobId, JobStatus.ERROR, null,
                    "No qualifying bean found for dependency 'org.quartz.Scheduler'")
        } else {
            job = updateStatus(dataMap.jobId, JobStatus.STARTED)
            quartzScheduler.scheduleJob(jobDetail, trigger)
        }

        return job
    }

    def exportData(ExportJobRepresentation exportJob, User user, Long jobId) {
        checkAccess(exportJob.constraint, user, PatientDataAccessLevel.MEASUREMENTS)
        def job = getJobById(jobId)
        if (job.jobStatus != JobStatus.CREATED.value) {
            throw new InvalidRequestException("Job with id $jobId has invalid status. " +
                    "Expected: $JobStatus.CREATED.value, actual: $job.jobStatus")
        }

        executeExportJob([
                user                         : user,
                jobName                      : job.jobName,
                jobId                        : jobId,
                constraint                   : exportJob.constraint,
                dataTypeAndFormatList        : exportJob.elements,
                includeMeasurementDateColumns: exportJob.includeMeasurementDateColumns,
                tableConfig                  : exportJob.tableConfig
        ])
    }

}
