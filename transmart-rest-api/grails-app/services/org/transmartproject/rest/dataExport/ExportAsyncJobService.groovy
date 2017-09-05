package org.transmartproject.rest.dataExport

import grails.transaction.Transactional
import org.grails.web.json.JSONObject
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.db.job.AsyncJobCoreDb
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.InvalidQueryException
import org.transmartproject.db.user.User

@Transactional
class ExportAsyncJobService {

    Scheduler quartzScheduler

    static String jobType = 'DataExport'

    AsyncJobCoreDb getJobById(Long id) {
        AsyncJobCoreDb.findById(id)
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
     * Cancel a running job
     */
    def cancelJob(id, group = null) {
        def jobStatus = JobStatus.CANCELLED
        log.debug("Attempting to delete ${id} from the Quartz scheduler")
        def result = quartzScheduler.deleteJob(new JobKey(id.toString, group))
        log.debug("Deletion attempt successful? ${result}")
        
        updateStatus(id, jobStatus)

        JSONObject jsonResult = new JSONObject()
        jsonResult.put("jobId", id)
        return jsonResult
    }

    String getJobUser(Long id) {
        def asyncJobCoreDb = AsyncJobCoreDb.findById(id)
        asyncJobCoreDb?.userId
    }

    AsyncJobCoreDb updateStatus(Long id, JobStatus status, String viewerURL = null, String results = null) {
        def asyncJobCoreDb = AsyncJobCoreDb.findById(id)
        if (isJobCancelled(asyncJobCoreDb)) return asyncJobCoreDb

        asyncJobCoreDb.jobStatus = status.value
        if (viewerURL?.trim()) asyncJobCoreDb.viewerURL = viewerURL
        if (results?.trim()) asyncJobCoreDb.results = results

        asyncJobCoreDb.save(flush: true)
        return asyncJobCoreDb
    }

    boolean isJobCancelled(AsyncJobCoreDb job) {
        boolean isJobCancelled = job.jobStatus == JobStatus.CANCELLED.value
        if (isJobCancelled) {
            log.warn("Job ${job.jobName} has been cancelled")
        }
        isJobCancelled
    }

    private static Map createJobDataMap(Constraint constraint, List types, User user, Long jobId, String jobName) {
        [
                user                 : user,
                jobName              : jobName,
                jobId                : jobId,
                constraint           : constraint,
                dataTypeAndFormatList: types
        ]
    }

    private AsyncJobCoreDb executeExportJob(Map dataMap) {

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
            job = updateStatus(dataMap.jobId, JobStatus.ERROR, null,
                    "No qualifying bean found for dependency 'org.quartz.Scheduler'")
        } else {
            job = updateStatus(dataMap.jobId, JobStatus.STARTED)
            quartzScheduler.scheduleJob(jobDetail, trigger)
        }

        return job
    }

    def exportData(Constraint constraint, List types, User user, Long jobId) {
        def job = getJobById(jobId)
        if (job.jobStatus != JobStatus.CREATED.value) {
            throw new InvalidRequestException("Job with id $jobId has invalid status. " +
                    "Expected: $JobStatus.CREATED.value, actual: $job.jobStatus")
        }

        def dataMap = createJobDataMap(constraint, types, user, jobId, job.jobName)
        executeExportJob(dataMap)
    }

}
