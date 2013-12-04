package jobs

import au.com.bytecode.opencsv.CSVWriter
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
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection

abstract class AnalysisJob implements Job {
    Map jobDataMap
    String name
    File temporaryDirectory
    final static String SUBSET1 = "subset1"
    final static String SUBSET2 = "subset2"
    final static Map<String, String> SHORT_NAME =
        [(AnalysisJob.SUBSET1): "S1", (AnalysisJob.SUBSET2): "S2"]
    final static Map<String, String> RESULT_INSTANCE_IDS =
        [(AnalysisJob.SUBSET1): "result_instance_id1", (AnalysisJob.SUBSET2): "result_instance_id2"]

    abstract protected void writeData(Map<String, TabularResult> results)

    abstract protected void runAnalysis()

    abstract protected Map<String, TabularResult> fetchResults()

    abstract protected void renderOutput()

    /**
     * This method is called by Quartz (never directly) and is the main method of the extending classes
     *
     * @param context
     * @throws JobExecutionException
     */
    @Override
    void execute(JobExecutionContext context) {
        if (isFoulJobName(context)) {
            throw new JobExecutionException("Job name mangled")
        }
        name = context.jobDetail.jobDataMap["jobName"]
        jobDataMap = context.jobDetail.jobDataMap

        try {
            setupTemporaryDirectory()

            writeParametersFile()
            Map<String, TabularResult> results = fetchResults()
            writeData(results)
            runAnalysis()
            renderOutput()
        } catch (Exception e) {
            log.error("Some exception occurred in the processing pipe", e)
            jobResultsService[name]["Exception"] = e.message
            updateStatus("Error")
        }
    }

    private static boolean isFoulJobName(JobExecutionContext context) {
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

    protected TabularResult fetchSubset(String subset) {
        if (jobDataMap[subset] == null) {
            return
        }

        HighDimensionDataTypeResource dataType = highDimensionResource.getSubResourceForType(
                jobDataMap.analysisConstraints["data_type"]
        )

        List<DataConstraint> dataConstraints = jobDataMap.analysisConstraints["dataConstraints"].collect { String constraintType, values ->
            if(values) {
                dataType.createDataConstraint(values, constraintType)
            }
        }.grep()

        List<AssayConstraint> assayConstraints = jobDataMap.analysisConstraints["assayConstraints"].collect { String constraintType, values ->
            if(values) {
                dataType.createAssayConstraint(values, constraintType)
            }
        }.grep()

        assayConstraints.add(
                dataType.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT, result_instance_id: jobDataMap[subset]
                )
        )

        Projection projection = dataType.createProjection([:], jobDataMap.analysisConstraints["projections"][0])
        return dataType.retrieveData(assayConstraints, dataConstraints, projection)
    }

    protected void withDefaultCsvWriter(Map<String, TabularResult> results, Closure constructFile) {
        try {
            File output = new File(temporaryDirectory, 'outputfile')
            output.createNewFile()
            output.withWriter { writer ->
                CSVWriter csvWriter = new CSVWriter(writer, '\t' as char)
                constructFile.call(csvWriter)
            }
        } finally {
            // close the tabularresults we processed (when not null)
            results.each {it.value?.close()}
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
        rConnection.eval("setwd('$temporaryDirectory')");

        //For each R step there is a list of commands.
        stepList.each { String currentCommand ->

            /**
             * Please make sure that any and all variables you add to the map here are placed _after_ the putAll
             * as otherwise you create a potential security vulnerability
             */
            Map vars = [:]
            vars.putAll jobDataMap
            escapeUserStrings(vars)

            vars.pluginDirectory = Holders.config.RModules.pluginScriptDirectory
            vars.temporaryDirectory = new File(temporaryDirectory, "subset1_" + study).absolutePath

            String finalCommand = processTemplates(currentCommand, vars)
            log.info "About to trigger R command:$finalCommand"
            // TODO Set back silent mode REXP rObject = rConnection.parseAndEval("try($finalCommand, silent=TRUE)")
            REXP rObject = rConnection.parseAndEval("try($finalCommand, silent=FALSE)")

            if (rObject.inherits("try-error")) {
                log.error "R command failure for:$finalCommand"
                handleRError(rObject, rConnection)
            }
        }
    }

    private static void escapeUserStrings(Map vars) {
        vars.each { k, v ->
            if (v.getClass() == String) {
                vars[k] = RUtil.escapeRStringContent(v)
            }
        }
    }

    private void handleRError(REXP rObject, RConnection rConnection) throws RserveException {
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
        asyncJobService.updateStatus(name, status, viewerUrl)
    }

    protected def getI2b2ExportHelperService() {
        jobDataMap.grailsApplication.mainContext.i2b2ExportHelperService
    }

    protected def getAsyncJobService() {
        jobDataMap.grailsApplication.mainContext.asyncJobService
    }

    protected def getJobResultsService() {
        jobDataMap.grailsApplication.mainContext.jobResultsService
    }

    protected HighDimensionResource getHighDimensionResource() {
        jobDataMap.grailsApplication.mainContext.getBean HighDimensionResource
    }
}
