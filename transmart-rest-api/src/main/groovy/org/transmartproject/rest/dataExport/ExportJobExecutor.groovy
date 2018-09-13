package org.transmartproject.rest.dataExport

import grails.persistence.support.PersistenceContextInterceptor
import grails.util.Holders
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.quartz.InterruptableJob
import org.quartz.JobExecutionContext
import org.quartz.UnableToInterruptJobException
import org.springframework.context.ApplicationContext
import org.transmartproject.core.users.User

import java.util.zip.ZipOutputStream

/**
 * This class will encompass the job scheduled by Quartz.
 */
@Slf4j
@CompileStatic
class ExportJobExecutor implements InterruptableJob, AutoCloseable {

    ApplicationContext ctx = Holders.applicationContext
    ExportService exportService = ctx.getBean('restExportService') as ExportService
    ExportAsyncJobService asyncJobService = ctx.getBean('exportAsyncJobService') as ExportAsyncJobService
    PersistenceContextInterceptor interceptor = ctx.getBean('persistenceInterceptor') as PersistenceContextInterceptor

    ZipOutputStream zipFile

    @Override
    void execute(JobExecutionContext jobExecutionContext) {
        Map jobDataMap = jobExecutionContext.jobDetail.getJobDataMap()
        def jobId = jobDataMap.jobId as Long
        def user = jobDataMap.user as User
        try {
            interceptor.init()
            zipData(jobDataMap)
            interceptor.flush()
        } catch (Throwable e) {
            log.error("Error during exporting data.", e)
            asyncJobService.updateStatus(jobId, user, JobStatus.ERROR, null, e.message)
        } finally {
            interceptor.destroy()
            close()
        }
    }

    private void zipData(Map jobDataMap) {
        def jobId = jobDataMap.jobId as Long
        def jobName = jobDataMap.jobName as String
        def user = jobDataMap.user as User
        String file = getFilePath(user, "${jobName}.zip")
        zipFile = new ZipOutputStream(new FileOutputStream(file))
        asyncJobService.updateStatus(jobId, user, JobStatus.GATHERING_DATA)
        exportService.exportData(jobDataMap, jobName, zipFile)
        asyncJobService.updateStatus(jobId, user, JobStatus.COMPLETED, file.toString())
    }

    private static File getFilePath(User user, String inputFileName) {
        new File(WorkingDirectory.forUser(user), inputFileName)
    }

    static InputStream getExportJobFileStream(String filePath) {

        InputStream inputStream = null
        try {
            File jobZipFile = new File(filePath)
            if (jobZipFile.isFile()) {
                inputStream = new FileInputStream(jobZipFile)
            }
        } catch (Exception e) {
            log.error("Failed to get the ZIP file", e)
        }

        inputStream
    }

    /**
     * Interrupts the job. Closes the output to the file to make job fail.
     * @throws UnableToInterruptJobException
     */
    @Override
    void interrupt() throws UnableToInterruptJobException {
        try {
            close()
        } catch (Exception e) {
            throw new UnableToInterruptJobException(e)
        }
    }

    @Override
    void close() throws Exception {
        if (zipFile) {
            zipFile.close()
        }
    }
}
