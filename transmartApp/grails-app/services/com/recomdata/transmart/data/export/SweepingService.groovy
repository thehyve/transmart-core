package com.recomdata.transmart.data.export

import com.recomdata.transmart.domain.i2b2.AsyncJob
import grails.transaction.Transactional

@Transactional
class SweepingService {

    def grailsApplication

    def sweep() {
        log.info "Triggering file sweep"
        def fileAge = grailsApplication.config.com.recomdata.export.jobs.sweep.fileAge;
        def now = new Date()
        def jobList = AsyncJob.createCriteria().list {
            eq("jobType", "DataExport")
            eq("jobStatus", "Completed")
            lt('lastRunOn', now - fileAge)
            //between('lastRunOn',now-fileAge, now)
        } as List<AsyncJob>

        def deleteDataFilesProcessor = new DeleteDataFilesProcessor()
        jobList.each { job ->
            if (deleteDataFilesProcessor.deleteDataFile(job.viewerURL, job.jobName)) {
                job.delete()
            }
        }
    }
}
