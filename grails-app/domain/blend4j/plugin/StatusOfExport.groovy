package blend4j.plugin

import groovy.time.TimeCategory
import groovy.time.TimeDuration

class StatusOfExport implements Serializable {

    final Set<String> TERMINATION_STATES = ['Completed', 'Cancelled', 'Error'] as Set

    String jobName
    String jobStatus
    String lastExportName
    Date lastExportTime

    static mapping = {
        table schema: 'galaxy', name: 'STATUS_OF_EXPORT_JOB'
        version false
        jobName column: 'JOB_NAME'
        jobStatus column: 'JOB_STATUS'
        lastExportName column: 'LAST_EXPORT_NAME'
        lastExportTime column : 'LAST_EXPORT_TIME'
    }

    static constraints = {
        jobName(nullable: false)
        jobStatus(nullable: false)
        lastExportName(nullable: false)
        lastExportTime(nullable: false)
    }

    TimeDuration getRunTime() {
        def lastTime = TERMINATION_STATES.contains(jobStatus) ?
                jobStatusTime : new Date()
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
