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

    ExportService exportService = ctx.restExportService
    ExportAsyncJobService asyncJobService = ctx.exportAsyncJobService

    final String tempFolderDirectory = Holders.config.com.recomdata.plugins.tempFolderDirectory

    public void execute(JobExecutionContext jobExecutionContext) {
        Map jobDataMap = jobExecutionContext.jobDetail.getJobDataMap()

        PersistenceContextInterceptor interceptor
        try {
            interceptor = Holders.applicationContext.persistenceInterceptor
            interceptor.init()
            zipData(jobDataMap)
        } catch (UnexpectedResultException e) {
            asyncJobService.updateStatus(jobDataMap.jobId, JobStatus.ERROR, null, e.message)
        } finally {
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
            exportService.exportData(jobDataMap, zipFile)
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

}
