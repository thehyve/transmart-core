package jobs

import grails.util.Holders
import org.quartz.Job
import org.quartz.JobDataMap
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.transmartproject.core.dataquery.highdim.HighDimensionResource

class AnalysisQuartzJobAdapter implements Job {

    public static final String PARAM_JOB_CLASS = 'jobClass'
    public static final String PARAM_GRAILS_APPLICATION = 'grailsApplication'
    public static final String PARAM_ANALYSIS_TYPE = 'analysis'
    public static final String PARAM_JOB_NAME = 'jobName'
    public static final String PARAM_ANALYSIS_CONSTRAINTS = 'analysisConstraints'

    JobDataMap jobDataMap

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        this.jobDataMap = context.jobDetail.jobDataMap

        AbstractAnalysisJob job = createAnalysisJob()
        try {
            job.run()
        } catch (Exception e) {
            job.log.error 'Some exception occurred in the processing pipe', e
            jobResultsService[job.name]['Exception'] = e.message
            job.updateStatus 'Error'
        }
    }

    AbstractAnalysisJob createAnalysisJob() {
        Class jobClass = jobDataMap.jobClass

        AbstractAnalysisJob result = jobClass.newInstance()

        /* wire things up; we could actually use some prototype bean here */
        result.dataTypeResource  = highDimensionResource.getSubResourceForType(
                jobDataMap[PARAM_ANALYSIS_CONSTRAINTS]['data_type'])

        result.updateStatus = { String status, String viewerUrl = null ->
            result.log.info "updateStatus called for status:$status, viewerUrl:$viewerUrl"
            asyncJobService.updateStatus result.name, status, viewerUrl
        }

        result.topTemporaryDirectory = new File(Holders.config.RModules.tempFolderDirectory)
        result.scriptsDirectory = new File(Holders.config.RModules.pluginScriptDirectory)

        result.name = jobDataMap[PARAM_JOB_NAME]

        result.studyName = i2b2ExportHelperService.
                findStudyAccessions jobDataMap.result_instance_id1

        Map userParams = new HashMap(jobDataMap)
        userParams.remove PARAM_JOB_CLASS
        userParams.remove PARAM_GRAILS_APPLICATION
        userParams.remove PARAM_ANALYSIS_TYPE /* doesn't matter at this point */
        userParams.remove PARAM_JOB_NAME /* save in RESULT.name */


        result.params = userParams

        result
    }

    def getAsyncJobService() {
        jobDataMap[PARAM_GRAILS_APPLICATION].mainContext.asyncJobService
    }

    def getJobResultsService() {
        jobDataMap[PARAM_GRAILS_APPLICATION].mainContext.jobResultsService
    }

    def getI2b2ExportHelperService() {
        jobDataMap[PARAM_GRAILS_APPLICATION].mainContext.i2b2ExportHelperService
    }

    HighDimensionResource getHighDimensionResource() {
        jobDataMap[PARAM_GRAILS_APPLICATION].mainContext.getBean HighDimensionResource
    }
}
