package jobs

import blend4j.plugin.GalaxyUserDetails
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
import nl.vumc.biomedbridges.core.WorkflowType
import nl.vumc.biomedbridges.galaxy.configuration.GalaxyConfiguration
import nl.vumc.biomedbridges.galaxy.HistoryUtils
import org.codehaus.groovy.grails.commons.GrailsApplication

import javax.annotation.PostConstruct

import static jobs.steps.AbstractDumpStep.DEFAULT_OUTPUT_FILE_NAME

/**
 * Note:
 * 1. Remember to register plugin module (searchapp.plugin_module):
 * insert into searchapp.plugin_module(module_seq, plugin_seq, name, module_name, form_page, params)
 * values (111, 1, 'RNA-Seq Differential Gene Expression (EdgeR)', 'DgeRNASeq', 'DgeRNASeq', '');
 * 2. Set up com.galaxy.blend4j.galaxyEnabled = true
 com.galaxy.blend4j.galaxyURL = 'url of the galaxy serve' in your config. file.
 * 3. Add your galaxy account to the transmart with the admin tab
 * */
@Component
@Scope('job')
class DgeRNASeq extends AbstractAnalysisJob {

    final static String GALAXY_WORKFLOW_NAME = 'rna-seq-dge'
    final static String EXPRESSION_MATRIX_INPUT_NAME = 'Expression Matrix'
    final static String DESIGN_MATRIX_INPUT_NAME = 'Design Matrix'
    final static String PHENODATA_FILE_NAME = 'phenodata.tsv'
    final static String CONTRAST_PARAM_NAME = 'analysis_type|contrast'
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
        def galaxyInstanceUrl = grailsApplication.config.com.galaxy.blend4j.galaxyURL
        String idOfTheUser = name.split('-')[0]
        def galaxyUser = GalaxyUserDetails.findByUsername(idOfTheUser)
        if (!galaxyUser) {
            throw new RuntimeException("No galaxy account found for ${idOfTheUser} user")
        }
        def apiKey = galaxyUser.getGalaxyKey()
        assert 'Galaxy credentials are not specified', galaxyInstanceUrl instanceof  String && apiKey instanceof String

        def historyName = params['jobName']
        boolean debug = grailsApplication.config.com.galaxy.debug ?: false
        def galaxyConfiguration = new GalaxyConfiguration().setDebug(debug)
        galaxyConfiguration.buildConfiguration(galaxyInstanceUrl, apiKey, historyName)
        workflowEngine = new DefaultWorkflowEngineFactory().getWorkflowEngine(WorkflowType.GALAXY, galaxyConfiguration, new HistoryUtils())
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
        new RNASeqReadCountDumpDataStep(
                temporaryDirectory: temporaryDirectory,
                resultsHolder: resultsHolder,
                params: params)
    }

    @Override
    protected List<String> getRStatements() {
        []
    }

    @Override
    protected getForwardPath() {
        return "/DgeRNASeq/RNASeqgroupTestOutput?jobName=${name}"
    }
}
