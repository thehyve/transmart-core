package jobs

import com.recomdata.transmart.util.RUtil
import grails.util.Holders
import groovy.text.SimpleTemplateEngine
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.rosuda.REngine.REXP
import org.rosuda.REngine.Rserve.RConnection
import org.rosuda.REngine.Rserve.RserveException
import org.transmartproject.core.dataquery.TabularResult

abstract class AnalysisJob implements Job {
    Map jobDataMap
    String name
    File temporaryDirectory

    abstract protected void writeData(TabularResult results)

    abstract protected void runAnalysis()

    abstract protected TabularResult fetchResults()

    abstract protected void renderOutput()

    /**
     * This method is called by Quartz (never directly) and is the main method of the extending classes
     *
     * @param context
     * @throws JobExecutionException
     */
    @Override
    void execute(JobExecutionContext context) {
        if (foulJobName(context)) {
            throw new JobExecutionContext("Jobname mangled")
        }
        name = context.jobDetail.jobDataMap["jobName"]
        jobDataMap = context.jobDetail.jobDataMap

        try {
            setupTemporaryDirectory()

            writeParametersFile()
            TabularResult results = fetchResults()
            writeData(results)
            runAnalysis()
            renderOutput()
        } catch (Exception e) {
            updateStatus("Error ${e.message}")
        }
    }

    private static boolean foulJobName(JobExecutionContext context) {
        if (context.jobDetail.jobDataMap["jobName"] ==~ /^[0-9A-Za-z-]+$/) {
            return false
        }
        return true
    }

    protected void setupTemporaryDirectory() {
        //FIXME: This is stupid of course, taking the 'name' from the client. What if the name is '../../'?
        temporaryDirectory = new File(new File(Holders.config.RModules.tempFolderDirectory, name), 'workingDirectory')
        temporaryDirectory.mkdirs()
    }

    /**
     * The file being written in this method is potentially used in the R scripts
     */
    protected void writeParametersFile() {
        File jobInfoFile = new File(temporaryDirectory, 'jobInfo.txt')

        jobInfoFile.withWriter { BufferedWriter it ->
            it.writeLine 'Parameters'
            jobDataMap.each { key, value ->
                it.writeLine "\t$key -> $value"
            }
        }
    }

    /**
     *
     * @param stepList  A list of R commands as Strings
     */
    protected void runRCommandList(List<String> stepList) {
        String study = i2b2ExportHelperService.findStudyAccessions([jobDataMap.result_instance_id1])

        //Establish a connection to R Server.
        RConnection rConnection = new RConnection();

        //Run the R command to set the working directory to our temp directory.
        rConnection.eval("setwd('${RUtil.escapeRStringContent(temporaryDirectory)}')");

        //For each R step there is a list of commands.
        stepList.each { String currentCommand ->

            /**
             * Please make sure that any and all variables you add to the map here are placed _after_ the putAll
             * as otherwise you create a potential security vulnerability
             */
            Map vars = [:]
            vars.putAll jobDataMap
            vars.pluginDirectory = Holders.config.RModules.pluginScriptDirectory
            log.info "pluginScriptDirectory:${Holders.config.RModules.pluginScriptDirectory}"
            vars.temporaryDirectory = new File(temporaryDirectory, "subset1_" + study).absolutePath

            vars.each { k, v -> vars[k] = RUtil.escapeRStringContent(v) }
            String finalCommand = processTemplates(currentCommand, vars)
            log.info "About to trigger R command:$finalCommand"
            // REXP rObject = rConnection.parseAndEval("try($finalCommand, silent=TRUE)")
            REXP rObject = rConnection.parseAndEval("try($finalCommand, silent=FALSE)")

            if (rObject.inherits("try-error")) {
                log.error "R command failure for:$finalCommand"
                handleError(rObject, rConnection)
            }
        }
    }

    private void handleError(REXP rObject, RConnection rConnection) throws RserveException {
        //Grab the error R gave us.
        String rError = rObject.asString()

        //This is the error we will eventually throw.
        RserveException newError = null

        //If it is a friendly error, use that, otherwise throw the default message.
        if (rError ==~ /.*\|\|FRIENDLY\|\|.*/) {
            rError = rError.replaceFirst(/.*\|\|FRIENDLY\|\|/, "")
            newError = new RserveException(rConnection, rError)
        } else {
            newError = new RserveException(rConnection, "There was an error running the R script for your job. Please contact an administrator.")
        }

        throw newError
    }

    protected static String processTemplates(String template, Map vars) {
        SimpleTemplateEngine engine = new SimpleTemplateEngine()
        engine.createTemplate(template).make(vars)
    }

    protected void updateStatus(String status, String viewerUrl = null) {
        log.info "updateStatus called for status:$status, viewerUrl:$viewerUrl"
        asyncJobService.updateStatus name, status, viewerUrl
    }

    protected def getI2b2ExportHelperService() {
        jobDataMap.grailsApplication.mainContext.i2b2ExportHelperService
    }

    protected def getAsyncJobService() {
        jobDataMap.grailsApplication.mainContext.asyncJobService
    }
}
