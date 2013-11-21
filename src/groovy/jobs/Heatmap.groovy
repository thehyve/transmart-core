package jobs

import org.quartz.Job
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import grails.util.Holders
import org.quartz.SimpleTrigger
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

    String temporaryDirectory = Holders.config.RModules.tempFolderDirectory

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        name = context.jobDetail.jobDataMap["jobName"]
        jobDataMap = context.jobDetail.jobDataMap

        File dir = setupTemporaryDirectory()
        writeParametersFile(dir)

        updateStatus('Gathering Data')
        TabularResult results = fetchResults()

        try {
            writeData(results, dir)
        } finally {
            results.close()
        }

        updateStatus('Running Analysis')
        runAnalysis()
        updateStatus('Rendering Output')
    }

    static void scheduleJob(params) {
        JobDetail jobDetail = new JobDetail(params.jobName, params.jobType, Heatmap.class)
        jobDetail.jobDataMap = new JobDataMap(params)
        SimpleTrigger trigger = new SimpleTrigger("triggerNow ${Calendar.instance.time.time}", 'RModules')
        Holders.grailsApplication.mainContext.quartzScheduler.scheduleJob(jobDetail, trigger)
    }

    private def runAnalysis() {}

    private def writeData(TabularResult results, File destinationDirectory) {
        File output = new File(destinationDirectory, 'outputfile')
        output.createNewFile()
        output.withWriter {
            CSVWriter writer = new CSVWriter(it, '\t' as char)

            writer.writeNext(['PATIENT_NUM', 'VALUE', 'GROUP'] as String[])

            results.rows.each { row ->
                writer.writeNext([row.patient_num, row.value, row.group] as String[])
            }

        }
    }

    private TabularResult fetchResults() {
        HighDimensionDataTypeResource dataType = highDimensionResource.getSubResourceForType(jobDataMap.divIndependentVariableType.toLowerCase())

        List<AssayConstraint> assayConstraints = [dataType.createAssayConstraint(AssayConstraint.PATIENT_SET_CONSTRAINT, result_instance_id: jobDataMap["result_instance_id1"])]
        assayConstraints.add(dataType.createAssayConstraint(AssayConstraint.ONTOLOGY_TERM_CONSTRAINT, concept_key: '\\\\Public Studies' + jobDataMap.variablesConceptPaths))

        List<DataConstraint> dataConstraints = [dataType.createDataConstraint([keyword_ids: [jobDataMap.divIndependentVariablePathway]], DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT)]

        Projection projection = dataType.createProjection([:], 'defaultRealProjection')

        dataType.retrieveData(assayConstraints, dataConstraints, projection)
    }

    private File setupTemporaryDirectory() {
        File jobWorkingDirectory = new File(new File(temporaryDirectory, name), 'workingDirectory')

        //TODO: This is stupid of course, taking the 'name' from the client. What if the name is '../../'?
        jobWorkingDirectory.mkdirs()

        jobWorkingDirectory
    }

    private void writeParametersFile(File jobWorkingDirectory) {
        File jobInfoFile = new File(jobWorkingDirectory, 'jobInfo.txt')

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

    private def getConceptsResourceService() {
        jobDataMap.grailsApplication.mainContext.getBean ConceptsResource
    }

    private def getAsyncJobService() {
        jobDataMap.grailsApplication.mainContext.asyncJobService
    }

    private HighDimensionResource getHighDimensionResource() {
        jobDataMap.grailsApplication.mainContext.getBean HighDimensionResource
    }
}
