package jobs

import jobs.steps.*
import jobs.steps.helpers.NumericColumnConfigurator
import jobs.steps.helpers.OptionalBinningColumnConfigurator
import jobs.steps.helpers.SimpleAddColumnConfigurator
import jobs.table.Table
import jobs.table.columns.PrimaryKeyColumn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.projections.Projection

import javax.annotation.PostConstruct

import static jobs.steps.AbstractDumpStep.DEFAULT_OUTPUT_FILE_NAME

@Component
@Scope('job')
class LogisticRegression extends AbstractAnalysisJob {

    @Autowired
    SimpleAddColumnConfigurator primaryKeyColumnConfigurator

    @Autowired
    NumericColumnConfigurator independentVariableConfigurator

    @Autowired
    @Qualifier('general')
    OptionalBinningColumnConfigurator outcomeVariableConfigurator

    @Autowired
    Table table

    @PostConstruct
    void init() {
        primaryKeyColumnConfigurator.column = new PrimaryKeyColumn(header: 'PATIENT_NUM')

        configureIndependentVariableConfigurator()
        configureOutcomeVariableConfigurator()
    }

    private void configureIndependentVariableConfigurator() {
        independentVariableConfigurator.header = 'Y'
        independentVariableConfigurator.setKeys('independent')
        independentVariableConfigurator.projection = Projection.LOG_INTENSITY_PROJECTION
        independentVariableConfigurator.multiRow   = true
    }

    private void configureOutcomeVariableConfigurator() {

        outcomeVariableConfigurator.header = 'X'
        outcomeVariableConfigurator.setKeys('groupBy')
        outcomeVariableConfigurator.projection          = Projection.LOG_INTENSITY_PROJECTION
        outcomeVariableConfigurator.multiRow            = true
        outcomeVariableConfigurator.binningConfigurator.setKeys('')
        def binningConfigurator = outcomeVariableConfigurator.binningConfigurator
        binningConfigurator.keyForDoBinning           = 'binning'
        binningConfigurator.keyForManualBinning       = 'manualBinning'
        binningConfigurator.keyForNumberOfBins        = 'numberOfBins'
        binningConfigurator.keyForBinDistribution     = 'binDistribution'
        binningConfigurator.keyForBinRanges           = 'binRanges'
        binningConfigurator.keyForVariableType        = 'variableType'

    }

    @Override
    protected List<Step> prepareSteps() {
        List<Step> steps = []

        steps << new BuildTableResultStep(
                table:         table,
                configurators: [primaryKeyColumnConfigurator,
                                outcomeVariableConfigurator,
                                independentVariableConfigurator])

        steps << new MultiRowAsGroupDumpTableResultsStep(
                table:              table,
                temporaryDirectory: temporaryDirectory,
                outputFileName:     DEFAULT_OUTPUT_FILE_NAME)

        steps << new RCommandsStep(
                temporaryDirectory: temporaryDirectory,
                scriptsDirectory:   scriptsDirectory,
                rStatements:        RStatements,
                studyName:          studyName,
                params:             params,
                extraParams:        [inputFileName: DEFAULT_OUTPUT_FILE_NAME])

        steps
    }

    @Override
    protected List<String> getRStatements() {
        [
            '''source('$pluginDirectory/LogisticRegression/LogisticRegressionLoader.R')''',
            '''LogisticRegressionData.loader(input.filename='outputfile.txt',
                        concept.dependent='$groupByVariable',
                        concept.independent='$independentVariable',
                        binning.enabled='FALSE',
                        binning.variable='')'''
        ]
    }

    @Override
    protected getForwardPath() {
        return "/logisticRegression/logisticRegressionOutput?jobName=$name"
    }
}
