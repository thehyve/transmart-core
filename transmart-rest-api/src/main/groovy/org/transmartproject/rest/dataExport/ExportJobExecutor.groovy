package org.transmartproject.rest.dataExport

import grails.persistence.support.PersistenceContextInterceptor
import grails.util.Holders

import groovy.util.logging.Slf4j
import org.apache.commons.lang.StringUtils
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

    DataExportService dataExportService = ctx.dataExportService
    ExportAsyncJobService asyncJobService = ctx.exportAsyncJobService
//    QuartzSpringScope quartzSpringScope = ctx.quartzSpringScope

    final String tempFolderDirectory = Holders.config.com.recomdata.plugins.tempFolderDirectory

    public void execute(JobExecutionContext jobExecutionContext) {
//        def userInContext = jobExecutionContext.jobDetail.jobDataMap['userInContext']
        Map jobDataMap = jobExecutionContext.jobDetail.getJobDataMap()

//        // put the user in context
//        quartzSpringScope.currentUserBeanQuartzScope = userInContext

        PersistenceContextInterceptor interceptor
        try {
            interceptor = Holders.applicationContext.persistenceInterceptor
            interceptor.init()
            zipData(jobDataMap)
        } catch (UnexpectedResultException e) {
            asyncJobService.updateStatus(jobDataMap.jobId, JobStatus.ERROR, null, e.message)
        } finally {
//            // Thread will be reused, need to clear user in context
//            quartzSpringScope.clear()
            interceptor.flush()
            interceptor.destroy()
        }

    }

    def zipData(Map jobDataMap) {

        Long jobId = jobDataMap.jobId
        String jobName = jobDataMap.jobName
        User user = jobDataMap.user
        String filePath = getFilePath(jobName, user.username)
        String fileName = filePath + ".zip"
        ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(fileName))

        try {
            asyncJobService.updateStatus(jobId, JobStatus.GATHERING_DATA)
            dataExportService.exportData(jobDataMap, zipFile)
        }
        catch (all) {
            log.error 'An exception occurred during data export job', all.message
            throw new UnexpectedResultException("An exception occurred during data export job: $all.message")
        }
        finally {
            zipFile.close()
        }
        asyncJobService.updateStatus(jobId, JobStatus.COMPLETED, fileName)
    }

    private String getFilePath(String inputFileName, String userName) {

        String jobTmpDirectory = ''
        if (StringUtils.isEmpty(tempFolderDirectory)) {
            jobTmpDirectory = '/var/tmp/jobs/'
        } else {
            jobTmpDirectory = tempFolderDirectory
        }
        String userDirectory = jobTmpDirectory + File.separator + userName
        File fileDirectory = new File(userDirectory)

        if(!fileDirectory.isDirectory()) fileDirectory.mkdirs()

        userDirectory + File.separator + inputFileName
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
