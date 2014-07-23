package jobs

import jobs.steps.*
import jobs.steps.helpers.CategoricalColumnConfigurator
import jobs.steps.helpers.HighDimensionColumnConfigurator
import jobs.steps.helpers.SimpleAddColumnConfigurator
import jobs.table.Table
import jobs.table.columns.PrimaryKeyColumn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import nl.vumc.biomedbridges.core.DefaultWorkflowEngineFactory
import nl.vumc.biomedbridges.core.Workflow
import nl.vumc.biomedbridges.core.WorkflowEngine
import nl.vumc.biomedbridges.core.WorkflowEngineFactory
import nl.vumc.biomedbridges.galaxy.configuration.GalaxyConfiguration
import org.codehaus.groovy.grails.commons.GrailsApplication

import javax.annotation.PostConstruct

import static jobs.steps.AbstractDumpStep.DEFAULT_OUTPUT_FILE_NAME

/**
 * Note:
 * 1. Remember to register plugin module (searchapp.plugin_module):
 * name: 'RNA-Seq Differential Gene Expression (EdgeR)'
 * module_name 'rnaSeqDge'
 * form_page: 'DgeRNASeq'
 * 2. Set up galaxy.instance_url and galaxy.api_key in your config. file.
 */
@Component
@Scope('job')
class RNASeqDge extends AbstractAnalysisJob {

    final static String GALAXY_WORKFLOW_NAME = 'rna-seq-dge'
    final static String EXPRESSION_MATRIX_INPUT_NAME = 'Expression Matrix'
    final static String DESIGN_MATRIX_INPUT_NAME = 'Design Matrix'
    final static String PHENODATA_FILE_NAME = 'phenodata.tsv'
    final static String CONTRAST_PARAM_NAME = 'contrast'
    final static int ANALYSIS_STEP_POSITION = 3

    @Autowired
    HighDimensionResource highDimensionResource

    @Autowired
    SimpleAddColumnConfigurator primaryKeyColumnConfigurator

    @Autowired
    CategoricalColumnConfigurator groupByConfigurator

    @Autowired
    HighDimensionColumnConfigurator highDimensionColumnConfigurator

    @Autowired
    GrailsApplication grailsApplication

    def thisJob = this
    WorkflowEngine workflowEngine
    Workflow workflow

    @PostConstruct
    void init() {
        def galaxyInstanceUrl = grailsApplication.config.galaxy.instance_url
        def apiKey = grailsApplication.config.galaxy.api_key
        assert 'Galaxy credentials are not specified', galaxyInstanceUrl && apiKey

        def historyName = params['jobName']
        def galaxyConfiguration = new GalaxyConfiguration()
        galaxyConfiguration.buildConfiguration(galaxyInstanceUrl, apiKey, historyName)
        workflowEngine = new DefaultWorkflowEngineFactory().getWorkflowEngine(WorkflowEngineFactory.GALAXY_TYPE, galaxyConfiguration)
        workflow = workflowEngine.getWorkflow(GALAXY_WORKFLOW_NAME)

        primaryKeyColumnConfigurator.column = new PrimaryKeyColumn(header: 'PATIENT_NUM')

        groupByConfigurator.header = 'group'
        groupByConfigurator.keyForConceptPaths = 'groupVariable'
    }

    @Autowired
    Table table

    @Override
    protected List<Step> prepareSteps() {
        List<Step> steps = []

        steps << new ParametersFileStep(
                temporaryDirectory: temporaryDirectory,
                params: params)

        steps << new BuildTableResultStep(
                table: table,
                configurators: [primaryKeyColumnConfigurator,
                        groupByConfigurator])

        steps << new SimpleDumpTableResultStep(table: table,
                temporaryDirectory: temporaryDirectory,
                outputFileName: PHENODATA_FILE_NAME
        )

        def openResultSetStep = new OpenHighDimensionalDataStep(
                params: params,
                dataTypeResource: highDimensionResource.getSubResourceForType(analysisConstraints['data_type']),
                analysisConstraints: analysisConstraints)

        steps << openResultSetStep

        steps << createDumpHighDimensionDataStep {-> openResultSetStep.results}

        steps << new Step() {
            String statusName = 'Running galaxy workflow'

            @Override
            void execute() {
                workflow.addInput(EXPRESSION_MATRIX_INPUT_NAME, new File(thisJob.temporaryDirectory, DEFAULT_OUTPUT_FILE_NAME))
                workflow.addInput(DESIGN_MATRIX_INPUT_NAME, new File(thisJob.temporaryDirectory, PHENODATA_FILE_NAME))
                workflow.setParameter(ANALYSIS_STEP_POSITION, CONTRAST_PARAM_NAME, thisJob.params['contrast'])
                workflowEngine.runWorkflow(workflow)
                def outputs = workflow.getOutputMap()
                outputs.each {
                    if(it.value instanceof File) {
                        it.value.renameTo(new File(thisJob.temporaryDirectory, it.value.name))
                    }
                }
            }
        }

        steps
    }

    protected Step createDumpHighDimensionDataStep(Closure resultsHolder) {
        new RNASeqDumpDataStep(
                temporaryDirectory: temporaryDirectory,
                resultsHolder: resultsHolder,
                params: params)
    }

    //TODO Is not needed for Galaxy analyses
    @Override
    protected List<String> getRStatements() {
        []
    }

    //TODO Returns empty page
    @Override
    protected getForwardPath() {
        return "/RNASeqgroupTest/RNASeqgroupTestOutput?jobName=${name}"
    }
}
