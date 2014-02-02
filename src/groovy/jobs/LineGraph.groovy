package jobs

import com.google.common.base.Function
import com.google.common.collect.Maps
import jobs.steps.*
import jobs.steps.helpers.ContextNumericVariableColumnConfigurator
import jobs.steps.helpers.OptionalBinningColumnConfigurator
import jobs.steps.helpers.SimpleAddColumnConfigurator
import jobs.table.ConceptTimeValuesTable
import jobs.table.Table
import jobs.table.columns.PrimaryKeyColumn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
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
    @Qualifier('general')
    OptionalBinningColumnConfigurator groupByColumnConfigurator

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

        measurementConfigurator.keyForConceptPath     = 'dependentVariable'
        measurementConfigurator.keyForDataType        = 'divDependentVariableType'
        measurementConfigurator.keyForSearchKeywordId = 'divDependentVariablePathway'

        groupByColumnConfigurator.columnHeader        = 'GROUP_VAR'
        groupByColumnConfigurator.projection          = Projection.DEFAULT_REAL_PROJECTION
        groupByColumnConfigurator.multiRow            = true
        groupByColumnConfigurator.setKeys 'groupBy'

        def binningConfigurator = groupByColumnConfigurator.binningConfigurator
        binningConfigurator.keyForDoBinning           = 'binningGroupBy'
        binningConfigurator.keyForManualBinning       = 'manualBinningGroupBy'
        binningConfigurator.keyForNumberOfBins        = 'numberOfBinsGroupBy'
        binningConfigurator.keyForBinDistribution     = 'binDistributionGroupBy'
        binningConfigurator.keyForBinRanges           = 'binRangesGroupBy'
        binningConfigurator.keyForVariableType        = 'variableTypeGroupBy'

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
                configurators: [primaryKeyColumnConfigurator,
                                measurementConfigurator,
                                groupByColumnConfigurator])

        steps << new LineGraphDumpTableResultsStep(
                table:              table,
                temporaryDirectory: temporaryDirectory)

        steps << new BuildConceptTimeValuesStep(
                table: conceptTimeValues,
                outputFile: new File(temporaryDirectory, SCALING_VALUES_FILENAME),
                header: [ "GROUP", "VALUE" ]
        )

        Map<String, Closure<String>> extraParams = [:]
        extraParams['scalingFilename'] = { getScalingFilename() }

        steps << new RCommandsStep(
                temporaryDirectory: temporaryDirectory,
                scriptsDirectory:   scriptsDirectory,
                rStatements:        RStatements,
                studyName:          studyName,
                params:             params,
                extraParams:        Maps.transformValues(extraParams, { it() } as Function))

        steps
    }

    private String getScalingFilename() {
        conceptTimeValues.resultMap ? SCALING_VALUES_FILENAME : null
    }

    @Override
    protected List<String> getRStatements() {
        [ '''source('$pluginDirectory/LineGraph/LineGraphLoader.r')''',
                '''LineGraph.loader(
                    input.filename           = 'outputfile',
                    graphType                = '$graphType',
                    scaling.filename  = ${scalingFilename == 'null' ? 'NULL' : "'$scalingFilename'"},
                    plot.individuals  = ${(plotIndividuals  == "true") ? 1 : 0 }
        )''' ]
    }

    @Override
    protected getForwardPath() {
        "/lineGraph/lineGraphOutput?jobName=$name"
    }
}
