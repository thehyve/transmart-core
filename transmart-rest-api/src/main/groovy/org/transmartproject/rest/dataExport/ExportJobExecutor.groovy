package org.transmartproject.rest.dataExport

import grails.persistence.support.PersistenceContextInterceptor
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.users.User

import java.util.zip.ZipOutputStream

/**
 * This class will encompass the job scheduled by Quartz.
 */
@Slf4j
class ExportJobExecutor implements Job {

    def ctx = Holders.grailsApplication.mainContext

    ExportService exportService = ctx.restExportService
    ExportAsyncJobService asyncJobService = ctx.exportAsyncJobService

    void execute(JobExecutionContext jobExecutionContext) {
        Map jobDataMap = jobExecutionContext.jobDetail.getJobDataMap()

        PersistenceContextInterceptor interceptor
        try {
            interceptor = Holders.applicationContext.persistenceInterceptor
            interceptor.init()
            zipData(jobDataMap)
        } catch (UnexpectedResultException e) {
            asyncJobService.updateStatus(jobDataMap.jobId, JobStatus.ERROR, null, e.message)
            log.error("Error during exporting data.", e)
        } finally {
            interceptor.flush()
            interceptor.destroy()
        }

    }

    def zipData(Map jobDataMap) {
        Long jobId = jobDataMap.jobId
        String jobName = jobDataMap.jobName
        User user = jobDataMap.user
        String file = getFilePath(user, "${jobName}.zip")
        ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(file))

        try {
            asyncJobService.updateStatus(jobId, JobStatus.GATHERING_DATA)
            exportService.exportData(jobDataMap, zipFile)
        } catch (Throwable e) {
            asyncJobService.updateStatus(jobId, JobStatus.ERROR, null, e.getMessage())
            log.error 'An exception occurred during data export job', e
            throw e
        } finally {
            zipFile.close()
        }
        asyncJobService.updateStatus(jobId, JobStatus.COMPLETED, file.toString())
    }

    private static File getFilePath(User user, String inputFileName) {
        new File(WorkingDirectory.forUser(user), inputFileName)
    }

    static InputStream getExportJobFileStream(String filePath) {

        InputStream inputStream = null
        try {
            File jobZipFile = new File(filePath);
            if (jobZipFile.isFile()) {
                inputStream = new FileInputStream(jobZipFile);
            }
        } catch (Exception e) {
            log.error("Failed to get the ZIP file", e)
        }

        inputStream
    }

}
