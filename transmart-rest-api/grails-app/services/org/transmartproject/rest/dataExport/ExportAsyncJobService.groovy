package org.transmartproject.rest.dataExport

import com.recomdata.transmart.domain.i2b2.AsyncJob
import grails.transaction.Transactional
import org.grails.web.json.JSONObject
import org.quartz.JobKey
import org.quartz.Scheduler
import org.transmartproject.db.multidimquery.query.InvalidQueryException
import org.transmartproject.db.user.User

@Transactional
class ExportAsyncJobService {

    Scheduler quartzScheduler

    static String jobType = 'DataExport'

    AsyncJob getJobById(Long id) {
        AsyncJob.findById(id)
    }

    List<AsyncJob> getJobList(User user) {
        // TODO add time constraints for retrived jobs
        if(user.admin) {
            AsyncJob.findAllByJobType(jobType)
        } else {
            AsyncJob.findAllByUserIdAndJobType(user.username, jobType)
        }
    }

    AsyncJob getJobByNameAndUser(String jobName, User user) {
        def jobs = AsyncJob.findAllByJobNameAndUserId(jobName, user.username)
        if(jobs.size() > 1) {
            throw new InvalidQueryException("More than one job with a name '$jobName' found")
        }
        jobs[0]
    }

    AsyncJob createNewJob(User user, String jobName = null) {
        def jobStatus = JobStatus.CREATED

        def newJob = new AsyncJob(lastRunOn: new Date())
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
        def asyncJob = AsyncJob.findById(id)
        asyncJob?.userId
    }

    AsyncJob updateStatus(Long id, JobStatus status, String viewerURL = null, String results = null) {
        def asyncJob = AsyncJob.findById(id)
        if (isJobCancelled(asyncJob)) return asyncJob

        asyncJob.jobStatus = status.value
        if (viewerURL?.trim()) asyncJob.viewerURL = viewerURL
        if (results?.trim()) asyncJob.results = results

        asyncJob.save(flush: true)
        return asyncJob
    }

    boolean isJobCancelled(AsyncJob job) {
        boolean isJobCancelled = job.jobStatus == JobStatus.CANCELLED.value
        if (isJobCancelled) {
            log.warn("Job ${job.jobName} has been cancelled")
        }
        isJobCancelled
    }
}
