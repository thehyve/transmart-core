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

    /**
     * Get the list of jobs to show in the jobs tab
     */
    List<AsyncJob> getJobList(User user) {
        // TODO add time constraints for retrived jobs
        if(user.admin) {
            AsyncJob.findAllByJobType(jobType)
        } else {
            AsyncJob.findAllByUserIdAndJobType(user.username, jobType)
        }
    }

    AsyncJob getJobByName(String jobName) {
        def jobs = AsyncJob.findAllByJobName(jobName)
        if(jobs.size() > 1) {
            throw new InvalidQueryException("More than one job with a name '$jobName' found")
        }
        jobs[0]
    }

    String getDefaultJobName(String userName, Long jobId) {
        userName + "-" + jobType + "-" + jobId
    }

    AsyncJob createNewJob(User user, String jobName = null) {
        def jobStatus = JobStatus.STARTED

        def newJob = new AsyncJob(lastRunOn: new Date())
        newJob.jobType = jobType
        newJob.jobStatus = jobStatus.value
        newJob.jobName = jobName
        newJob.userId = user.username
        newJob.save()

        if (!jobName?.trim()) {
            newJob.jobName = getDefaultJobName(user.username, newJob.id)
            newJob.save()
        }

        newJob
    }
    
    /**
     * Cancel a running job
     */
    def cancelJob(jobName, group = null) {
        def jobStatus = JobStatus.CANCELLED
        log.debug("Attempting to delete ${jobName} from the Quartz scheduler")
        def result = quartzScheduler.deleteJob(new JobKey(jobName, group))
        log.debug("Deletion attempt successful? ${result}")
        
        updateStatus(jobName, jobStatus)

        JSONObject jsonResult = new JSONObject()
        jsonResult.put("jobName", jobName)
        return jsonResult
    }

    String checkJobStatus(String jobName) {
        def asyncJob = AsyncJob.findByJobName(jobName)
        asyncJob?.jobStatus
    }

    String getJobUser(String jobName) {
        def asyncJob = AsyncJob.findByJobName(jobName)
        asyncJob?.userId
    }
    
    def updateStatus(String jobName, JobStatus status, String viewerURL = null, String results = null) {
        def asyncJob = AsyncJob.findByJobName(jobName)
        if (isJobCancelled(asyncJob)) return true

        asyncJob.jobStatus = status.value
        if (viewerURL?.trim()) asyncJob.viewerURL = viewerURL
        if (results?.trim()) asyncJob.results = results

        asyncJob.save(flush: true)
        return false
    }

    boolean isJobCancelled(AsyncJob job) {
        boolean isJobCancelled = job.jobStatus == JobStatus.CANCELLED.value
        if (isJobCancelled) {
            log.warn("Job ${job.jobName} has been cancelled")
        }
        isJobCancelled
    }
}
