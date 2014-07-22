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
        independentVariableConfigurator.alwaysClinical = true
        independentVariableConfigurator.setKeys('independent')
    }

    private void configureOutcomeVariableConfigurator() {
        outcomeVariableConfigurator.header = 'X'
        outcomeVariableConfigurator.setKeys('groupBy')
        outcomeVariableConfigurator.projection          = Projection.DEFAULT_REAL_PROJECTION
        outcomeVariableConfigurator.multiRow            = true

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

        steps << new ParametersFileStep(
                temporaryDirectory: temporaryDirectory,
                params: params)

        steps << new BuildTableResultStep(
                table:         table,
                configurators: [primaryKeyColumnConfigurator,
                        outcomeVariableConfigurator,
                        independentVariableConfigurator])

        steps << new SimpleDumpTableResultStep(
                table:              table,
                temporaryDirectory: temporaryDirectory
        )

        steps << new RCommandsStep(
                temporaryDirectory: temporaryDirectory,
                scriptsDirectory:   scriptsDirectory,
                rStatements:        RStatements,
                studyName:          studyName,
                params:             params
        )

        steps
    }

    @Override
    protected List<String> getRStatements() {
        [
            '''source('$pluginDirectory/LogisticRegression/LogisticRegressionLoader.R')''',
            '''LogisticRegressionData.loader(input.filename='outputfile.txt',
                        concept.dependent='$dependentVariable',
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
