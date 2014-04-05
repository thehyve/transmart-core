package jobs

import com.github.jmchilton.blend4j.galaxy.*
import com.github.jmchilton.blend4j.galaxy.beans.*
import jobs.steps.BuildTableResultStep
import jobs.steps.ParametersFileStep
import jobs.steps.SimpleDumpTableResultStep
import jobs.steps.Step
import jobs.steps.helpers.NumericColumnConfigurator
import jobs.steps.helpers.SimpleAddColumnConfigurator
import jobs.table.Table
import jobs.table.columns.PrimaryKeyColumn
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

import static jobs.steps.AbstractDumpStep.DEFAULT_OUTPUT_FILE_NAME

/**
 * Note: It's work in progress.
 * For now to make this job work you need 2 prerequisites:
 * 1. Register galaxy plugin with name 'Galaxy'(searchapp.plugin)
 * and plugin module (searchapp.plugin_module) with name 'histogram'
 * 2. Specify your galaxy api credentials: galaxy.instance and galaxy.api_key
 */
@Component
@Scope('job')
class Histogram extends AbstractAnalysisJob {

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    SimpleAddColumnConfigurator primaryKeyColumnConfigurator

    @Autowired
    NumericColumnConfigurator numericVariableConfigurator

    @Autowired
    Table table

    final static GALAXY_WORKFLOW_ID = 'histogram'
    final static GALAXY_WORKFLOW_INPUT_TITLE = 'Input Dataset'

    private def thisJob = this

    private GalaxyInstance galaxyInstance
    private WorkflowsClient workflowsClient
    private HistoriesClient historiesClient
    private ToolsClient toolsClient

    private String historyId
    private WorkflowOutputs outputs
    private Map<String, String> filenameIdMap = [:]

    @Override
    protected getForwardPath() {
        "/Histogram/histogramOut?jobName=${name}"
    }

    @Override
    protected List<Step> prepareSteps() {
        List<Step> steps = []

        steps << new ParametersFileStep(
                temporaryDirectory: temporaryDirectory,
                params: params)

        steps << new BuildTableResultStep(
                table: table,
                configurators: [primaryKeyColumnConfigurator,
                                numericVariableConfigurator])

        steps << new SimpleDumpTableResultStep(table: table,
                temporaryDirectory: temporaryDirectory,
                outputFileName: DEFAULT_OUTPUT_FILE_NAME,
                noQuotes: true
        )

        steps << new Step() {
            String statusName = 'Uploading to galaxy'

            @Override
            void execute() {
                historyId = historiesClient.create(new History(thisJob.name)).id

                thisJob.temporaryDirectory.eachFile {
                    ToolExecution exec = toolsClient.upload(new ToolsClient.FileUploadRequest(historyId, it))
                    filenameIdMap = filenameIdMap + exec.outputs.collectEntries { [(it.name): it.id] }
                }
                //TODO Find better way
                while (!historiesClient.showHistory(historyId).ready) {
                    Thread.sleep(1000)
                }
            }
        }

        steps << new Step() {
            String statusName = 'Running galaxy workflow'

            @Override
            void execute() {
                WorkflowInputs inputs = new WorkflowInputs()
                def workflows = workflowsClient.workflows
                Workflow workflow = workflows.find { it.name == GALAXY_WORKFLOW_ID }
                assert 'could not find workflow', workflow
                WorkflowDetails workflowDetails = workflowsClient.showWorkflow(workflow.id)
                def inputId = workflowDetails.inputs.find { it.value.label == GALAXY_WORKFLOW_INPUT_TITLE }.key

                def input = new WorkflowInputs.WorkflowInput(filenameIdMap[DEFAULT_OUTPUT_FILE_NAME], WorkflowInputs.InputSourceType.HDA)
                inputs.setInput(inputId, input)
                inputs.destination = new WorkflowInputs.ExistingHistory(historyId)
                inputs.workflowId = workflow.id

                outputs = workflowsClient.runWorkflow(inputs)

                //TODO Find better way
                while (!historiesClient.showHistory(outputs.historyId).ready) {
                    Thread.sleep(1000)
                }
            }
        }

        steps << new Step() {
            String statusName = 'Downloading results from galaxy'

            @Override
            void execute() {
                assert 'No output', outputs
                outputs.outputIds.each { String outputId ->
                    Dataset dataset = historiesClient.showDataset(historyId, outputId)
                    def resultUrl = new URL("${galaxyInstance.galaxyUrl}/datasets/${dataset.id}/display/?to_ext=${dataset.dataType}")
                    def out = new BufferedOutputStream(new FileOutputStream(new File(thisJob.temporaryDirectory, "${dataset.id}.${dataset.dataType}")))
                    out << resultUrl.openStream()
                    out.close()
                }
            }
        }

        steps
    }

    @PostConstruct
    void init() {
        //TODO Register plugin/plugin module automatically?
        //Plugin.find
        //def is = this.class.classLoader.getResourceAsStream("galaxy/workflows/${GALAXY_WORKFLOW_ID}.ga")
        //TODO Ensure workflow is deployed to galaxy

        galaxyInstance = GalaxyInstanceFactory.get(grailsApplication.config.galaxy.instance, grailsApplication.config.galaxy.api_key)
        workflowsClient = galaxyInstance.workflowsClient
        historiesClient = galaxyInstance.historiesClient
        toolsClient = galaxyInstance.toolsClient

        primaryKeyColumnConfigurator.column = new PrimaryKeyColumn(header: 'PATIENT_NUM')

        numericVariableConfigurator.header = 'VALUE'
        numericVariableConfigurator.keyForConceptPath = 'variablesConceptPath'
        numericVariableConfigurator.alwaysClinical = true
    }

}
