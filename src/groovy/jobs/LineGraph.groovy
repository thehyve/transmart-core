package jobs

import jobs.steps.*
import jobs.steps.helpers.CategoricalColumnConfigurator
import jobs.steps.helpers.ContextNumericVariableColumnConfigurator
import jobs.steps.helpers.SimpleAddColumnConfigurator
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
    ContextNumericVariableColumnConfigurator measurementConfigurator

    @Autowired
    Table table

    @PostConstruct
    void init() {
        primaryKeyColumnConfigurator.column = new PrimaryKeyColumn(header: 'PATIENT_NUM')

        measurementConfigurator.columnHeader          = 'VALUE'
        measurementConfigurator.projection            = Projection.DEFAULT_REAL_PROJECTION
        measurementConfigurator.multiRow              = true
        measurementConfigurator.multiConcepts         = true
        // we do not want group name pruning for LineGraph
        measurementConfigurator.pruneConceptPath      = false

        measurementConfigurator.keyForConceptPath     = "dependentVariable"
        measurementConfigurator.keyForDataType        = "divDependentVariableType"
        measurementConfigurator.keyForSearchKeywordId = "divDependentVariablePathway"

        groupByColumnConfigurator.columnHeader        = 'GROUP_VAR'
        groupByColumnConfigurator.keyForConceptPaths  = 'groupByVariable'
    }

    @Override
    protected List<Step> prepareSteps() {
        List<Step> steps = []

        steps << new ParametersFileStep(
                temporaryDirectory: temporaryDirectory,
                params: params)

        steps << new BuildTableResultStep(
                table:         table,
                configurators: [primaryKeyColumnConfigurator, measurementConfigurator, groupByColumnConfigurator])

        steps << new LineGraphDumpTableResultsStep(
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
