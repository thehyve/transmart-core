package jobs

import jobs.steps.BuildConceptTimeValuesStep
import jobs.steps.BuildTableResultStep
import jobs.steps.MultiRowAsGroupDumpTableResultsStep
import jobs.steps.ParametersFileStep
import jobs.steps.RCommandsStep
import jobs.steps.Step
import jobs.steps.helpers.SingleOrMultiNumericVariableColumnConfigurator
import jobs.steps.helpers.SimpleAddColumnConfigurator
import jobs.steps.helpers.CategoricalColumnConfigurator
import jobs.table.ConceptTimeValuesTable
import jobs.table.MissingValueAction
import jobs.table.Table
import jobs.table.columns.PrimaryKeyColumn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.ontology.ConceptsResource

import javax.annotation.PostConstruct
import java.security.InvalidParameterException

@Component
@Scope('job')
class LineGraph extends AbstractAnalysisJob {

    private static final String SCALING_VALUES_FILENAME = 'conceptScaleValues'

    @Autowired
    SimpleAddColumnConfigurator primaryKeyColumnConfigurator

    @Autowired
    CategoricalColumnConfigurator groupByColumnConfigurator

    @Autowired
    SingleOrMultiNumericVariableColumnConfigurator dependentVariableConfigurator

    @Autowired
    ConceptTimeValuesTable conceptTimeValues

    @Autowired
    Table table

    @PostConstruct
    void init() {
        primaryKeyColumnConfigurator.column = new PrimaryKeyColumn(header: 'PATIENT_NUM')

        dependentVariableConfigurator.columnHeader          = 'VALUE'
        dependentVariableConfigurator.projection            = Projection.DEFAULT_REAL_PROJECTION
        dependentVariableConfigurator.missingValueAction    = new MissingValueAction.DropRowMissingValueAction()
        dependentVariableConfigurator.multiRow              = true
        dependentVariableConfigurator.keyForIsCategorical = 'dependentVariableCategorical'
        // we do not want group name pruning for LineGraph
        dependentVariableConfigurator.isGroupNamePruningNecessary = false

        dependentVariableConfigurator.keyForConceptPath     = "dependentVariable"
        dependentVariableConfigurator.keyForDataType        = "divDependentVariableType"
        dependentVariableConfigurator.keyForSearchKeywordId = "divDependentVariablePathway"

        groupByColumnConfigurator.columnHeader = 'GROUP_VAR'
        groupByColumnConfigurator.keyForConceptPaths = 'groupByVariable'

        conceptTimeValues.conceptPaths = dependentVariableConfigurator.getConceptPaths()
        conceptTimeValues.enabledClosure = { -> !Boolean.parseBoolean(params.getProperty('plotEvenlySpaced')) }
    }

    @Override
    protected List<Step> prepareSteps() {
        List<Step> steps = []

        steps << new ParametersFileStep(
                temporaryDirectory: temporaryDirectory,
                params: params)

        steps << new BuildTableResultStep(
                table:         table,
                configurators: [primaryKeyColumnConfigurator, dependentVariableConfigurator, groupByColumnConfigurator])

        steps << new MultiRowAsGroupDumpTableResultsStep(
                table:              table,
                temporaryDirectory: temporaryDirectory)

        conceptTimeValues.outputFile = new File(temporaryDirectory, SCALING_VALUES_FILENAME)

        steps << new BuildConceptTimeValuesStep(table: conceptTimeValues)

        Map<String, Closure<String>> lazyExtraParams = [:]
        lazyExtraParams['scalingFilename'] = { getScalingFilename() }

        steps << new RCommandsStep(
                temporaryDirectory: temporaryDirectory,
                scriptsDirectory:   scriptsDirectory,
                rStatements:        RStatements,
                studyName:          studyName,
                params:             params,
                lazyExtraParams:    lazyExtraParams)

        steps
    }

    private String getScalingFilename() {
        conceptTimeValues.hasScaling() ? SCALING_VALUES_FILENAME : null
    }

    @Override
    protected List<String> getRStatements() {
        [ '''source('$pluginDirectory/LineGraph/LineGraphLoader.r')''',
                '''LineGraph.loader(
                    input.filename    = 'outputfile',
                    graphType         = '$graphType',
                    scaling.filename  = ${scalingFilename == 'null' ? 'NULL' : "'$scalingFilename'"},
                    plot.individuals  = ${(plotIndividuals  == "true") ? 1 : 0 }
        )''' ]
    }
    // HDD.data.type            = '${divDependentVariableType!="CLINICAL"?projections:null}',

    @Override
    protected getForwardPath() {
        "/lineGraph/lineGraphOutput?jobName=$name"
    }
}
