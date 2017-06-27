package org.transmartproject.rest.dataExport

import grails.persistence.support.PersistenceContextInterceptor
import grails.util.Holders

import groovy.util.logging.Slf4j
import org.apache.commons.lang.StringUtils
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.transmartproject.core.exceptions.UnexpectedResultException

import java.util.zip.ZipOutputStream

/**
 * This class will encompass the job scheduled by Quartz.
 */
@Slf4j
class ExportJobExecutor implements Job {

    def ctx = Holders.grailsApplication.mainContext

    RestDataExportService restDataExportService = ctx.restDataExportService
    ExportAsyncJobService asyncJobService = ctx.exportAsyncJobService
//    QuartzSpringScope quartzSpringScope = ctx.quartzSpringScope

    final String tempFolderDirectory = Holders.config.com.recomdata.plugins.tempFolderDirectory

    public void execute(JobExecutionContext jobExecutionContext) {
        def userInContext = jobExecutionContext.jobDetail.jobDataMap['userInContext']
        Map jobDataMap = jobExecutionContext.jobDetail.getJobDataMap()

//        // put the user in context
//        quartzSpringScope.currentUserBeanQuartzScope = userInContext

        PersistenceContextInterceptor interceptor
        try {
            interceptor = Holders.applicationContext.persistenceInterceptor
            interceptor.init()
            zipData(jobDataMap)
        } catch (UnexpectedResultException e) {
            asyncJobService.updateStatus(jobDataMap.jobName, JobStatus.ERROR, null, e)
        } finally {
//            // Thread will be reused, need to clear user in context
//            quartzSpringScope.clear()
            interceptor.flush()
            interceptor.destroy()
        }

    }

    def zipData(Map jobDataMap) {

        String jobName = jobDataMap.jobName
        String filePath = getFilePath(jobName)
        String fileName = filePath + ".zip"
        ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(fileName))

        try {
            asyncJobService.updateStatus(jobName, JobStatus.GATHERING_DATA)
            restDataExportService.exportData(jobDataMap, zipFile)
        }
        catch (all) {
            log.error 'An exception occurred during data export job', all.message
            throw new UnexpectedResultException("An exception occurred during data export job: $all.message")
        }
        finally {
            zipFile.close()
        }
        asyncJobService.updateStatus(jobName, JobStatus.COMPLETED, fileName)
    }

    private String getFilePath(String inputFileName) {

        String jobTmpDirectory = ''
        if (StringUtils.isEmpty(tempFolderDirectory)) {
            jobTmpDirectory = '/var/tmp/jobs/'
        } else {
            jobTmpDirectory = tempFolderDirectory
        }

        jobTmpDirectory + File.separator + inputFileName
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
