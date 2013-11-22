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
    }

    static void scheduleJob(Map params) {
        JobDetail jobDetail = new JobDetail(params.jobName, params.jobType, Heatmap.class)
        jobDetail.jobDataMap = new JobDataMap(params)
        SimpleTrigger trigger = new SimpleTrigger("triggerNow ${Calendar.instance.time.time}", 'RModules')
        quartzScheduler.scheduleJob(jobDetail, trigger)
    }

    private def runAnalysis() {
        updateStatus('Running Analysis')

        Closure<GString> source = {
            "source('$pluginDirectory/Heatmap/HeatmapLoader.R')"
        }

        Closure<GString> createHeatmap = {
            """
            Heatmap.loader(
                            input.filename = 'outputfile',
                            imageWidth     = as.integer('$txtImageWidth'),
                            imageHeight    = as.integer('$txtImageHeight'),
                            pointsize      = as.integer('$txtImagePointsize'),
                            maxDrawNumber  = as.integer('$txtMaxDrawNumber'))
            """
        }

        runRCommandList([source, createHeatmap])
    }

    private static Object escapingWrapper(Map<String, String> vars) {
        Object escapingWrapper = new Object()
        escapingWrapper.metaClass.getProperty { name ->
            if (name == null) {
                ''
            } else {
                RUtil.escapeRStringContent(vars[name])
            }
        }
        return escapingWrapper
    }

    private void runRCommandList(List<Closure<GString>> stepList) {
        String study = i2b2ExportHelperService.findStudyAccessions([jobDataMap.result_instance_id1])

        //Establish a connection to R Server.
        RConnection rConnection = new RConnection();

        //Run the R command to set the working directory to our temp directory.
        rConnection.eval("setwd('$temporaryDirectory')");

        //For each R step there is a list of commands.
        stepList.each { Closure<GString> currentCommand ->

            /**
             * Please make sure that any and all variables you add to the map here are placed _after_ the putAll
             * as otherwise you create a potential security vulnerability
             */
            Map vars = [:]
            vars.putAll jobDataMap
            vars.pluginDirectory = Holders.config.RModules.pluginScriptDirectory
            vars.temporaryDirectory = new File(temporaryDirectory, "subset1_" + study).absolutePath

            def finalCommand = Heatmap.escapingWrapper(vars).with {
                currentCommand.call().toString()
            }

            REXP rObject = rConnection.parseAndEval("try($finalCommand, silent=TRUE)")

            if (rObject.inherits("try-error")) {
                handleError(rObject)
            }
        }
    }

    private void handleError(REXP rObject) throws RserveException {
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

    private def writeData(TabularResult results) {
        try {
            File output = new File(temporaryDirectory, 'outputfile')
            output.createNewFile()
            output.withWriter {
                CSVWriter writer = new CSVWriter(it, '\t' as char)

                writer.writeNext(['PATIENT_NUM', 'VALUE', 'GROUP'] as String[])

                results.rows.each { row ->
                    row.assayIndexMap.each { assay, index ->
                        writer.writeNext([assay.assay.subjectId, row.data[index], "${row.probe}_${row.geneSymbol}"] as String[])
                    }
                }
            }
        } finally {
            results.close()
        }
    }

    private TabularResult fetchResults() {
        updateStatus('Gathering Data')

        HighDimensionDataTypeResource dataType = highDimensionResource.getSubResourceForType(jobDataMap.divIndependentVariableType.toLowerCase())

        List<AssayConstraint> assayConstraints = [dataType.createAssayConstraint(AssayConstraint.PATIENT_SET_CONSTRAINT, result_instance_id: jobDataMap["result_instance_id1"])]
        assayConstraints.add(dataType.createAssayConstraint(AssayConstraint.ONTOLOGY_TERM_CONSTRAINT, concept_key: '\\\\Public Studies' + jobDataMap.variablesConceptPaths))

        List<DataConstraint> dataConstraints = [dataType.createDataConstraint([keyword_ids: [jobDataMap.divIndependentVariablePathway]], DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT)]

        Projection projection = dataType.createProjection([:], 'default_real_projection')

        dataType.retrieveData(assayConstraints, dataConstraints, projection)
    }

    private File setupTemporaryDirectory() {
        //FIXME: This is stupid of course, taking the 'name' from the client. What if the name is '../../'?
        temporaryDirectory = new File(new File(Holders.config.RModules.tempFolderDirectory, name), 'workingDirectory')
        temporaryDirectory.mkdirs()
    }

    private void writeParametersFile() {
        File jobInfoFile = new File(temporaryDirectory, 'jobInfo.txt')

        jobInfoFile.withWriter { BufferedWriter it ->
            it.writeLine 'Parameters'
            jobDataMap.each { key, value ->
                it.writeLine "\t$key -> $value"
            }
        }
    }

    private void updateStatus(String status) {
        asyncJobService.updateStatus name, status
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
