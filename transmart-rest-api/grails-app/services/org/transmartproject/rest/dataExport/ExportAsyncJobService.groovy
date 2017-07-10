package org.transmartproject.rest.dataExport

import grails.transaction.Transactional
import org.grails.web.json.JSONObject
import org.quartz.JobKey
import org.quartz.Scheduler
import org.transmartproject.db.job.AsyncJobCoreDb
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
        def AsyncJobCoreDb = AsyncJobCoreDb.findById(id)
        AsyncJobCoreDb?.userId
    }

    AsyncJobCoreDb updateStatus(Long id, JobStatus status, String viewerURL = null, String results = null) {
        def AsyncJobCoreDb = AsyncJobCoreDb.findById(id)
        if (isJobCancelled(AsyncJobCoreDb)) return AsyncJobCoreDb

        AsyncJobCoreDb.jobStatus = status.value
        if (viewerURL?.trim()) AsyncJobCoreDb.viewerURL = viewerURL
        if (results?.trim()) AsyncJobCoreDb.results = results

        AsyncJobCoreDb.save(flush: true)
        return AsyncJobCoreDb
    }

    boolean isJobCancelled(AsyncJobCoreDb job) {
        boolean isJobCancelled = job.jobStatus == JobStatus.CANCELLED.value
        if (isJobCancelled) {
            log.warn("Job ${job.jobName} has been cancelled")
        }
        isJobCancelled
    }
}
