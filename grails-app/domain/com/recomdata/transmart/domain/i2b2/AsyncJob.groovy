package com.recomdata.transmart.domain.i2b2

class AsyncJob {
    String jobName
    String jobStatus
    String runTime
    Date lastRunOn
    String viewerURL
    String altViewerURL
    String results
    String jobType

    static mapping = {
        table 'I2B2DEMODATA.ASYNC_JOB'
        version false
        jobName column: 'JOB_NAME'
        jobStatus column: 'JOB_STATUS'
        runTime column: 'RUN_TIME'
        lastRunOn column: 'LAST_RUN_ON'
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
}