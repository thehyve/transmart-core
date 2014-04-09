package jobs

import jobs.steps.*
import jobs.steps.helpers.NumericColumnConfigurator
import jobs.steps.helpers.SimpleAddColumnConfigurator
import jobs.table.Table
import jobs.table.columns.PrimaryKeyColumn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.projections.Projection

import javax.annotation.PostConstruct
import java.security.InvalidParameterException

import static jobs.steps.AbstractDumpStep.DEFAULT_OUTPUT_FILE_NAME

@Component
@Scope('job')
class ScatterPlot extends AbstractAnalysisJob {

    @Autowired
    SimpleAddColumnConfigurator primaryKeyColumnConfigurator

    @Autowired
    NumericColumnConfigurator independentVariableConfigurator

    @Autowired
    NumericColumnConfigurator dependentVariableConfigurator

    @Autowired
    Table table

    @PostConstruct
    void init() {
        primaryKeyColumnConfigurator.column =
                new PrimaryKeyColumn(header: 'PATIENT_NUM')

        configureConfigurator independentVariableConfigurator, 'independent', 'X'
        configureConfigurator dependentVariableConfigurator,   'dependent',   'Y'

        /* we also need these two extra variables (see R statements) */
        extraParamValidation()
    }

    private void extraParamValidation() {
        if (params['divIndependentVariablePathway'] != null) {
            if (params['divIndependentPathwayName'] == null) {
                throw new InvalidParameterException(
                        'Missing user parameter "divIndependentPathwayName"')
            }
        }

        if (params['divDependentVariablePathway'] != null) {
            if (params['divDependentPathwayName'] == null) {
                throw new InvalidParameterException(
                        'Missing user parameter "divDependentPathwayName"')
            }
        }
    }

    private void configureConfigurator(NumericColumnConfigurator configurator,
                                       String key,
                                       String header) {
        configurator.header     = header
        configurator.projection = Projection.LOG_INTENSITY_PROJECTION
        configurator.multiRow   = true
        configurator.setKeys(key)
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
                        independentVariableConfigurator,
                        dependentVariableConfigurator,])

        steps << new MultiRowAsGroupDumpTableResultsStep(
                table:              table,
                temporaryDirectory: temporaryDirectory,
                outputFileName: DEFAULT_OUTPUT_FILE_NAME)

        steps << new RCommandsStep(
                temporaryDirectory: temporaryDirectory,
                scriptsDirectory:   scriptsDirectory,
                rStatements:        RStatements,
                studyName:          studyName,
                params:             params,
                extraParams: [inputFileName: DEFAULT_OUTPUT_FILE_NAME])

        steps
    }

    @Override
    protected List<String> getRStatements() {
        [ '''source('$pluginDirectory/ScatterPlot/ScatterPlotLoader.R')''',
                '''ScatterPlot.loader(
                    input.filename               = '$inputFileName',
                    concept.dependent            = '$dependentVariable',
                    concept.independent          = '$independentVariable',
                    concept.dependent.type       = '$divDependentVariableType',
                    concept.independent.type     = '$divIndependentVariableType',
                    genes.dependent              = '$divDependentPathwayName',
                    genes.independent            = '$divIndependentPathwayName',
                    aggregate.probes.independent = '$divIndependentVariableprobesAggregation' == 'true',
                    aggregate.probes.dependent   = '$divDependentVariableprobesAggregation'   == 'true',
                    snptype.dependent            = '',
                    snptype.independent          = '',
        )''' ] // last two params should be removed
    }

    @Override
    protected getForwardPath() {
        "/scatterPlot/scatterPlotOut?jobName=$name"
    }
}
