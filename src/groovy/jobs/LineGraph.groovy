package jobs

import jobs.steps.*
import jobs.steps.helpers.CategoricalColumnConfigurator
import jobs.steps.helpers.SimpleAddColumnConfigurator
import jobs.steps.helpers.SingleOrMultiNumericVariableColumnConfigurator
import jobs.table.MissingValueAction
import jobs.table.Table
import jobs.table.columns.PrimaryKeyColumn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.projections.Projection

import javax.annotation.PostConstruct

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

        dependentVariableConfigurator.columnHeader          = 'VALUE'
        dependentVariableConfigurator.projection            = Projection.DEFAULT_REAL_PROJECTION
        dependentVariableConfigurator.missingValueAction    = new MissingValueAction.DropRowMissingValueAction()
        dependentVariableConfigurator.multiRow              = true
        dependentVariableConfigurator.keyForIsCategorical   = 'dependentVariableCategorical'
        // we do not want group name pruning for LineGraph
        dependentVariableConfigurator.pruneConceptPath      = false

        dependentVariableConfigurator.keyForConceptPath     = "dependentVariable"
        dependentVariableConfigurator.keyForDataType        = "divDependentVariableType"
        dependentVariableConfigurator.keyForSearchKeywordId = "gexpathway"

        groupByColumnConfigurator.columnHeader              = 'GROUP_VAR'
        groupByColumnConfigurator.keyForConceptPaths        = 'groupByVariable'
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
                    graphType                = '$graphType',
                    plot.individuals         = ${(plotIndividuals=="true")?1:0})''']
    }

    @Override
    protected getForwardPath() {
        "/lineGraph/lineGraphOutput?jobName=$name"
    }
}
