package jobs

import jobs.steps.*
import jobs.steps.helpers.BinningColumnConfigurator
import jobs.steps.helpers.CategoricalOrBinnedColumnConfigurator
import jobs.steps.helpers.SimpleAddColumnConfigurator
import jobs.table.MissingValueAction
import jobs.table.Table
import jobs.table.columns.PrimaryKeyColumn
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.projections.Projection

@Component
@Scope('job')
class TableWithFisher extends AbstractAnalysisJob implements InitializingBean {

    @Autowired
    SimpleAddColumnConfigurator primaryKeyColumnConfigurator

    @Autowired
    CategoricalOrBinnedColumnConfigurator independentVariableConfigurator

    @Autowired
    CategoricalOrBinnedColumnConfigurator dependentVariableConfigurator

    @Autowired
    Table table

    @Override
    void afterPropertiesSet() throws Exception {
        primaryKeyColumnConfigurator.column =
                new PrimaryKeyColumn(header: 'PATIENT_NUM')

        configureConfigurator independentVariableConfigurator, 'indep', 'independent', 'X'
        configureConfigurator dependentVariableConfigurator,   'dep',   'dependent',   'Y'
    }

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
                table: table,
                temporaryDirectory: temporaryDirectory)

        steps << new RCommandsStep(
                temporaryDirectory: temporaryDirectory,
                scriptsDirectory: scriptsDirectory,
                rStatements: RStatements,
                studyName: studyName,
                params: params)

        steps
    }

    private void configureConfigurator(CategoricalOrBinnedColumnConfigurator configurator,
                                       String keyPart,
                                       String longKeyPart,
                                       String header) {
        configurator.columnHeader          = header
        configurator.projection            = Projection.DEFAULT_REAL_PROJECTION
        configurator.missingValueAction    = new MissingValueAction.DropRowMissingValueAction()

        configurator.keyForConceptPaths    = "${longKeyPart}Variable"
        configurator.keyForDataType        = "div${longKeyPart.capitalize()}VariableType"
        configurator.keyForSearchKeywordId = "div${longKeyPart.capitalize()}VariablePathway"

        configurator.multiRow              = true

        BinningColumnConfigurator binningColumnConfigurator =
                configurator.binningConfigurator
        binningColumnConfigurator.keyForDoBinning       = "binning${keyPart.capitalize()}"
        binningColumnConfigurator.keyForManualBinning   = "manualBinning${keyPart.capitalize()}"
        binningColumnConfigurator.keyForNumberOfBins    = "numberOfBins${keyPart.capitalize()}"
        binningColumnConfigurator.keyForBinDistribution = "binDistribution${keyPart.capitalize()}"
        binningColumnConfigurator.keyForBinRanges       = "binRanges${keyPart.capitalize()}"
        binningColumnConfigurator.keyForVariableType    = "variableType${keyPart.capitalize()}"
    }

    @Override
    protected List<String> getRStatements() {
        [ '''source('$pluginDirectory/TableWithFisher/FisherTableLoader.R')''',
                '''FisherTable.loader(input.filename='outputfile')''' ]
    }

    @Override
    protected getForwardPath() {
        """/tableWithFisher/fisherTableOut?jobName=$name"""
    }
}
