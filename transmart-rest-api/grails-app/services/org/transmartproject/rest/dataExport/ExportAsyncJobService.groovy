package org.transmartproject.rest.dataExport

import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Restrictions
import org.quartz.*
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User
import org.transmartproject.db.clinical.AbstractDataResourceService
import org.transmartproject.db.job.AsyncJobCoreDb
import org.transmartproject.db.multidimquery.query.InvalidQueryException
import org.transmartproject.core.multidimquery.export.ExportJobRepresentation

@Slf4j
@Transactional
@CompileStatic
class ExportAsyncJobService extends AbstractDataResourceService {

    static final String EXPORT_JOB_TYPE = 'DataExport'

    @Autowired
    Scheduler quartzScheduler

    @Autowired
    SessionFactory sessionFactory


    @Transactional(readOnly = true)
    AsyncJobCoreDb getJobById(Long id, User user) {
        def job = DetachedCriteria.forClass(AsyncJobCoreDb)
                .add(Restrictions.idEq(id))
                .add(Restrictions.eq('userId', user.username))
                .add(Restrictions.eq('jobType', EXPORT_JOB_TYPE))
                .getExecutableCriteria(sessionFactory.currentSession)
                .uniqueResult() as AsyncJobCoreDb

        if (!job) {
            throw new NoSuchResourceException("Job with id '$id' does not exist.")
        }
        return job
    }

    @Transactional(readOnly = true)
    List<AsyncJobCoreDb> getJobList(User user) {
        DetachedCriteria.forClass(AsyncJobCoreDb)
                .add(Restrictions.eq('userId', user.username))
                .add(Restrictions.eq('jobType', EXPORT_JOB_TYPE))
                .getExecutableCriteria(sessionFactory.currentSession)
                .list() as List<AsyncJobCoreDb>
    }

    AsyncJobCoreDb createNewJob(String jobName, User user) {
        // Check input
        jobName = jobName?.trim() ? escapeInvalidFileNameChars(jobName) : null

        // Save job
        def newJob = new AsyncJobCoreDb(lastRunOn: new Date())
        newJob.jobType = EXPORT_JOB_TYPE
        newJob.jobStatus = JobStatus.CREATED.value
        newJob.jobName = jobName
        newJob.userId = user.username
        newJob.save()

        // Rename job to job id if no job name has been specified
        if (!jobName) {
            newJob.jobName = newJob.id.toString()
            newJob.save()
        }

        // Check for uniqueness of the job name for the user
        def jobCount = DetachedCriteria.forClass(AsyncJobCoreDb)
                .add(Restrictions.eq('jobName', newJob.jobName))
                .add(Restrictions.eq('userId', user.username))
                .getExecutableCriteria(sessionFactory.currentSession)
                .setProjection(Projections.rowCount())
                .uniqueResult() as Integer
        if (jobCount > 1) {
            throw new InvalidQueryException("Job with name '$jobName' already exists")
        }

        newJob
    }

    /**
     * Cancels a job
     */
    void cancelJob(Long jobId, User user) {
        def job = getJobById(jobId, user)
        log.info("Cancelling job with id ${jobId}.")
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
        updateStatus(job, JobStatus.CANCELLED)
        log.info("${jobKey} job was marked as cancelled.")
    }

    /**
     * Deletes a job after the cancelling it
     */
    void deleteJob(Long jobId, User user) {
        def job = getJobById(jobId, user)
        if (!job.inTerminalStatus) {
            log.info("Job with id ${jobId} hasn't finished. Try to cancel it first.")
            try {
                cancelJob(jobId, user)
            } catch (Exception e) {
                log.error("Can't cancel job with id ${jobId}.", e)
            }
        }
        log.info("Removing job with id ${jobId} from the table.")
        job.delete()
    }

    AsyncJobCoreDb updateStatus(AsyncJobCoreDb job, JobStatus status, String viewerURL = null, String results = null) {
        if (job.jobStatus == JobStatus.CANCELLED.value) {
            log.warn("Job ${job.jobName} has been cancelled")
            return job
        }

        job.jobStatus = status.value
        if (viewerURL?.trim()) {
            job.viewerURL = viewerURL
        }
        if (results?.trim()) {
            job.results = results
        }

        job.save(flush: true)
        return job
    }

    AsyncJobCoreDb updateStatus(Long jobId, User user, JobStatus status, String viewerURL = null, String results = null) {
        def job = getJobById(jobId, user)
        updateStatus(job, status, viewerURL, results)
    }

    private static JobKey getJobKeyForId(Long jobId) {
        new JobKey(jobId.toString(), 'DataExport')
    }

    private static escapeInvalidFileNameChars(String fileName) {
        // escape characters that are not permited to be used in file names (Windows+Linux/Unix)
        fileName?.replaceAll("[/\\\\?%*:|\\\"<>]", "_")
    }

    private AsyncJobCoreDb executeExportJob(AsyncJobCoreDb job, Map dataMap) {

        def jobDataMap = new JobDataMap(dataMap)
        JobDetail jobDetail = JobBuilder.newJob(ExportJobExecutor.class)
                .withIdentity(getJobKeyForId(job.id))
                .setJobData(jobDataMap)
                .build()
        def randomDelay = Math.random()*10 as int
        def startTime = new Date(new Date().time + randomDelay)

        def trigger = TriggerBuilder.newTrigger()
                .startAt(startTime)
                .withIdentity("${dataMap.jobId.toString()}-Trigger", 'DataExport')
                .build()

        if (!quartzScheduler) {
            job = updateStatus(job, JobStatus.ERROR, null,
                    "No qualifying bean found for dependency 'org.quartz.Scheduler'")
        } else {
            job = updateStatus(job, JobStatus.STARTED)
            quartzScheduler.scheduleJob(jobDetail, trigger)
        }

        return job
    }

    def exportData(ExportJobRepresentation exportJob, Long jobId, User user) {
        def job = getJobById(jobId, user)
        if (job.jobStatus != JobStatus.CREATED.value) {
            throw new InvalidRequestException("Job with id $jobId has invalid status. " +
                    "Expected: $JobStatus.CREATED.value, actual: $job.jobStatus")
        }

        checkAccess(exportJob.constraint, user, PatientDataAccessLevel.MEASUREMENTS)

        executeExportJob(job, [
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
