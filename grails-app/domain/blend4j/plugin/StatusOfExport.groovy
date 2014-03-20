package blend4j.plugin

class StatusOfExport {

    String id
    String jobStatus
    String lastExportName
    Date lastExportTime

    static mapping = {
        table schema: 'galaxy', name: 'status_of_export_job'
        version false
        id column: 'JOB_NAME_ID'
        jobStatus column: 'JOB_STATUS'
        lastExportName column: 'LAST_EXPORT_NAME'
        lastExportTime column : 'LAST_EXPORT_TIME'
    }

    static constraints = {
        id(nullable: false)
        jobStatus(nullable: false)
        lastExportName(nullable: false)
        lastExportTime(nullable: false)
    }

}
