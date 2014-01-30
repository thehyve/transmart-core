package jobs

import jobs.steps.*
import jobs.steps.helpers.CategoricalColumnConfigurator
import jobs.steps.helpers.ContextNumericVariableColumnConfigurator
import jobs.steps.helpers.SimpleAddColumnConfigurator
import jobs.table.ConceptTimeValuesTable
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

    private static final String SCALING_VALUES_FILENAME = 'conceptScaleValues'

    @Autowired
    SimpleAddColumnConfigurator primaryKeyColumnConfigurator

    @Autowired
    CategoricalColumnConfigurator groupByColumnConfigurator

    @Autowired
    ContextNumericVariableColumnConfigurator measurementConfigurator

    @Autowired
    ConceptTimeValuesTable conceptTimeValues

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
        
        conceptTimeValues.conceptPaths = measurementConfigurator.getConceptPaths()
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
                configurators: [primaryKeyColumnConfigurator, measurementConfigurator, groupByColumnConfigurator])

        steps << new LineGraphDumpTableResultsStep(
                table:              table,
                temporaryDirectory: temporaryDirectory)

        //set here and not in @PostConstruct because temporaryDirectory is not initialized at that moment
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

    static class LineGraphDumpTableResultsStep extends MultiRowAsGroupDumpTableResultsStep {

        protected void addGroupColumnHeaders() {
            // should be called only once
            headers << 'GROUP'        // concept path
            headers << 'PLOT_GROUP'  // row label (e.g. probe)
        }

        protected createDecoratingIterator() {
            new TwoColumnExpandingMapIterator(preResults, transformedColumnsIndexes)
        }
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

    @Override
    protected getForwardPath() {
        "/lineGraph/lineGraphOutput?jobName=$name"
    }
}
