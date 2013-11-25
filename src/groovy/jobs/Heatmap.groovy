package jobs

import com.recomdata.transmart.util.RUtil
import org.quartz.Job
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import grails.util.Holders
import org.quartz.SimpleTrigger
import org.rosuda.REngine.REXP
import org.rosuda.REngine.Rserve.RConnection
import org.rosuda.REngine.Rserve.RserveException
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.ontology.ConceptsResource
import au.com.bytecode.opencsv.CSVWriter
import groovy.text.SimpleTemplateEngine

class Heatmap implements Job {

    Map jobDataMap
    String name

    File temporaryDirectory

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        name = context.jobDetail.jobDataMap["jobName"]
        jobDataMap = context.jobDetail.jobDataMap

        setupTemporaryDirectory()

        writeParametersFile()
        TabularResult results = fetchResults()
        writeData(results)
        runAnalysis()
        renderOutput()
    }

    //TODO: Move to to be created job scheduler. Or perhaps we can live with this code in the controller
    static void scheduleJob(Map params) {
        JobDetail jobDetail = new JobDetail(params.jobName, params.jobType, Heatmap.class)
        jobDetail.jobDataMap = new JobDataMap(params)
        SimpleTrigger trigger = new SimpleTrigger("triggerNow ${Calendar.instance.time.time}", 'RModules')
        quartzScheduler.scheduleJob(jobDetail, trigger)
    }

    private void runAnalysis() {
        updateStatus('Running Analysis')

        String source = 'source(\'$pluginDirectory/Heatmap/HeatmapLoader.R\')'

        String createHeatmap = '''Heatmap.loader(
                            input.filename = \'outputfile\',
                            imageWidth     = as.integer(\'$txtImageWidth\'),
                            imageHeight    = as.integer(\'$txtImageHeight\'),
                            pointsize      = as.integer(\'$txtImagePointsize\'),
                            maxDrawNumber  = as.integer(\'$txtMaxDrawNumber\'))'''

        runRCommandList([source, createHeatmap])
    }

    //TODO: Move to abstract Job class
    private void renderOutput() {
        updateStatus('Completed', "?jobName=${name}")
    }

    private String processTemplates(String template, Map vars) {
        def engine = new SimpleTemplateEngine()
        engine.createTemplate(template).make(vars)
    }

    //TODO: Move to abstract Job class
    private void runRCommandList(List<String> stepList) {
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
            vars.pluginDirectory = Holders.config.RModules.pluginScriptDirectory
            log.info "pluginScriptDirectory:${Holders.config.RModules.pluginScriptDirectory}"
            vars.temporaryDirectory = new File(temporaryDirectory, "subset1_" + study).absolutePath

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

    //TODO: Move to abstract Job class
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

    //TODO: Move to abstract Job class and extract writing of the header and row
    private void writeData(TabularResult results) {
        try {
            File output = new File(temporaryDirectory, 'outputfile')
            output.createNewFile()
            output.withWriter {
                CSVWriter writer = new CSVWriter(it, '\t' as char)

                writer.writeNext(['PATIENT_NUM', 'VALUE', 'GROUP'] as String[])

                results.rows.each { row ->
                    row.assayIndexMap.each { assay, index ->
                        // TODO Handle subsets properly
                        writer.writeNext(
                                ['S1_'+assay.assay.patientInTrialId, row.data[index], "${row.probe}_${row.geneSymbol}"] as String[]
                        )
                    }
                }
            }
        } finally {
            results.close()
        }
    }

    private TabularResult fetchResults() {
        updateStatus('Gathering Data')

        HighDimensionDataTypeResource dataType = highDimensionResource.getSubResourceForType(
                jobDataMap.divIndependentVariableType.toLowerCase()
        )

        List<AssayConstraint> assayConstraints = [
                dataType.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT, result_instance_id: jobDataMap["result_instance_id1"]
                )
        ]
        assayConstraints.add(
                dataType.createAssayConstraint(
                        AssayConstraint.ONTOLOGY_TERM_CONSTRAINT, concept_key: '\\\\Public Studies' + jobDataMap.variablesConceptPaths
                )
        )

        List<DataConstraint> dataConstraints = [
                dataType.createDataConstraint(
                        [keyword_ids: [jobDataMap.divIndependentVariablePathway]], DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT
                )
        ]

        Projection projection = dataType.createProjection([:], 'default_real_projection')

        dataType.retrieveData(assayConstraints, dataConstraints, projection)
    }

    //TODO: Move to abstract Job class
    private void setupTemporaryDirectory() {
        //FIXME: This is stupid of course, taking the 'name' from the client. What if the name is '../../'?
        temporaryDirectory = new File(new File(Holders.config.RModules.tempFolderDirectory, name), 'workingDirectory')
        temporaryDirectory.mkdirs()
    }

    //TODO: Move to abstract Job class
    private void writeParametersFile() {
        File jobInfoFile = new File(temporaryDirectory, 'jobInfo.txt')

        jobInfoFile.withWriter { BufferedWriter it ->
            it.writeLine 'Parameters'
            jobDataMap.each { key, value ->
                it.writeLine "\t$key -> $value"
            }
        }
    }

    //TODO: Move to abstract Job class
    private void updateStatus(String status, String viewerUrl = null) {
        asyncJobService.updateStatus name, status, viewerUrl
    }

    //TODO: can't type this because of the circular dependency on transmartApp
    /*private def getJobResultsService() {
        data.grailsApplication.applicationContext.getBean('jobResultsService')
    }*/

    private static def getQuartzScheduler() {
        Holders.grailsApplication.mainContext.quartzScheduler
    }

    private def getConceptsResourceService() {
        jobDataMap.grailsApplication.mainContext.getBean ConceptsResource
    }

    private def getI2b2ExportHelperService() {
        jobDataMap.grailsApplication.mainContext.i2b2ExportHelperService
    }

    private def getAsyncJobService() {
        jobDataMap.grailsApplication.mainContext.asyncJobService
    }

    private HighDimensionResource getHighDimensionResource() {
        jobDataMap.grailsApplication.mainContext.getBean HighDimensionResource
    }
}
