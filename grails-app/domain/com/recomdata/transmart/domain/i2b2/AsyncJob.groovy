package com.recomdata.transmart.domain.i2b2

import groovy.time.TimeCategory
import groovy.time.TimeDuration

class AsyncJob {

    final Set<String> TERMINATION_STATES = ['Completed', 'Cancelled', 'Error'] as Set

    Long id
    String jobName
    String jobStatus
    String runTime
    Date lastRunOn
    Date jobStatusTime
    String viewerURL
    String altViewerURL
    String results
    String jobType

    static mapping = {
        table 'I2B2DEMODATA.ASYNC_JOB'
        version false
        id column: 'ID',
                generator: 'increment',
                params: [sequence: 'I2B2DEMODATA.ASYNC_JOB_SEQ']
        jobName column: 'JOB_NAME'
        jobStatus column: 'JOB_STATUS'
        runTime column: 'RUN_TIME'
        lastRunOn column: 'LAST_RUN_ON'
        jobStatusTime column: 'JOB_STATUS_TIME'
        viewerURL column: 'VIEWER_URL'
        altViewerURL column: 'ALT_VIEWER_URL'
        results column: 'JOB_RESULTS'
        jobType column: 'JOB_TYPE'
    }

    static constraints = {
        jobName(nullable: true)
        jobStatus(nullable: true)
        runTime(nullable: true)
        viewerURL(nullable: true)
        altViewerURL(nullable: true)
        results(nullable: true)
    }

    TimeDuration getRunTime() {
        def lastTime = TERMINATION_STATES.contains(jobStatus) ? this.jobStatusTime : new Date()
        lastRunOn && lastTime ? TimeCategory.minus(lastTime, lastRunOn) : null
    }

    void setJobStatus(String jobStatus) {
        if (this.jobStatus == jobStatus) {
            return
        }
        this.jobStatusTime = new Date()
        this.jobStatus = jobStatus
    }

}