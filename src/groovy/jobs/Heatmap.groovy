package jobs

import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import grails.util.Holders
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection

class Heatmap implements Job {
    Map data
    String name
    HighDimensionResource highDimensionResource

    String temporaryDirectory = Holders.config.RModules.tempFolderDirectory

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        name = context.jobDetail.jobDataMap["jobName"]
        data = context.jobDetail.jobDataMap

        File dir = setupTemporaryDirectory()
        writeParametersFile(dir)

        updateStatus('Gathering Data')
        TabularResult results = getData()
        writeData(results)

        updateStatus('Running Analysis')
        updateStatus('Rendering Output')
    }

    private writeData(TabularResult results) {}

    private TabularResult getData() {
        HighDimensionDataTypeResource dataType = highDimensionResource.knownDataTypes[data["divIndependentVariableType"]]

        List<AssayConstraint> assayConstraints = [dataType.createAssayConstraint(AssayConstraint.PATIENT_SET_CONSTRAINT, result_instance_id: data["result_instance_id1"])]
        assayConstraints.add(dataType.createAssayConstraint(AssayConstraint.ONTOLOGY_TERM_CONSTRAINT, term: data["variablesConceptPaths"]))

        List<DataConstraint> dataConstraints = [dataType.createDataConstraint([keyword_ids: [data["divIndependentVariablePathway"]]], DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT)]

        Projection projection = dataType.createProjection([:], 'defaultRealProjection')

        dataType.retrieveData(assayConstraints, dataConstraints, projection)
    }

    private File setupTemporaryDirectory() {
        File jobWorkingDirectory = new File(new File(temporaryDirectory, name), 'workingDirectory')

        if (!jobWorkingDirectory.mkdirs()) {
            throw new RuntimeException('harm is confident this will never fail')
        }

        jobWorkingDirectory
    }

    private void writeParametersFile(File jobWorkingDirectory) {
        File jobInfoFile = new File(jobWorkingDirectory, 'jobInfo.txt')

        jobInfoFile.withWriter { BufferedWriter it ->
            it.writeLine 'Parameters'
            data.each { key, value ->
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

    private def getAsyncJobService() {
        data.grailsApplication.applicationContext.getBean 'asyncJobService'
    }
}
