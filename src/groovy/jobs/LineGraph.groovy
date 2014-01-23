package jobs

import jobs.steps.BuildTableResultStep
import jobs.steps.MultiRowAsGroupDumpTableResultsStep
import jobs.steps.ParametersFileStep
import jobs.steps.RCommandsStep
import jobs.steps.Step
import jobs.steps.helpers.SingleOrMultiNumericVariableColumnConfigurator
import jobs.steps.helpers.SimpleAddColumnConfigurator
import jobs.steps.helpers.CategoricalColumnConfigurator
import jobs.table.MissingValueAction
import jobs.table.Table
import jobs.table.columns.PrimaryKeyColumn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.projections.Projection

import javax.annotation.PostConstruct
import java.security.InvalidParameterException

@Component
@Scope('job')
class LineGraph extends AbstractAnalysisJob {

    @Autowired
    SimpleAddColumnConfigurator primaryKeyColumnConfigurator

    @Autowired
    CategoricalColumnConfigurator groupByColumnConfigurator

    @Autowired
    SingleOrMultiNumericVariableColumnConfigurator dependentVariableConfigurator

    @Autowired
    Table table

    @PostConstruct
    void init() {
        primaryKeyColumnConfigurator.column = new PrimaryKeyColumn(header: 'PATIENT_NUM')

        configureConfigurator dependentVariableConfigurator,   'dependent', 'VALUE'

        groupByColumnConfigurator.columnHeader = 'GROUP_VAR'
        groupByColumnConfigurator.keyForConceptPaths = 'groupByVariable'
    }

    private void configureConfigurator(SingleOrMultiNumericVariableColumnConfigurator configurator,
                                       String key,
                                       String header) {
        configurator.columnHeader          = header
        configurator.projection            = Projection.DEFAULT_REAL_PROJECTION
        configurator.missingValueAction    = new MissingValueAction.DropRowMissingValueAction()
        configurator.multiRow              = true
        configurator.keyForIsCategorical = 'dependentVariableCategorical'
        // we do not want group name pruning for LineGraph
        configurator.isGroupNamePruningNecessary = false

        configurator.keyForConceptPath     = "dependentVariable"
        configurator.keyForDataType        = "div${key.capitalize()}VariableType"
        configurator.keyForSearchKeywordId = "div${key.capitalize()}VariablePathway"
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

        steps << new RCommandsStep(
                temporaryDirectory: temporaryDirectory,
                scriptsDirectory:   scriptsDirectory,
                rStatements:        RStatements,
                studyName:          studyName,
                params:             params)

        steps
    }

    @Override
    protected List<String> getRStatements() {
        [ '''source('$pluginDirectory/LineGraph/LineGraphLoader.r')''',
                '''LineGraph.loader(
                    input.filename           = 'outputfile',
        )''' ]
    }

    @Override
    protected getForwardPath() {
        "/lineGraph/lineGraphOutput?jobName=$name"
    }
}
