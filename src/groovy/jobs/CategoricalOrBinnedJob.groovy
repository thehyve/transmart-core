package jobs

import jobs.steps.*
import jobs.steps.helpers.BinningColumnConfigurator
import jobs.steps.helpers.OptionalBinningColumnConfigurator
import jobs.steps.helpers.ColumnConfigurator
import jobs.steps.helpers.SimpleAddColumnConfigurator
import jobs.table.MissingValueAction
import jobs.table.Table
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.projections.Projection

abstract class CategoricalOrBinnedJob extends AbstractAnalysisJob implements InitializingBean {

    @Autowired
    SimpleAddColumnConfigurator primaryKeyColumnConfigurator

    abstract ColumnConfigurator getIndependentVariableConfigurator()

    abstract ColumnConfigurator getDependentVariableConfigurator()

    @Autowired
    Table table

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

    protected void configureConfigurator(OptionalBinningColumnConfigurator configurator,
                                         String keyBinPart,
                                         String keyVariablePart,
                                         String header = null) {
        if (header != null) {
            configurator.columnHeader = header
        }
        configurator.projection            = Projection.DEFAULT_REAL_PROJECTION
        configurator.missingValueAction    = new MissingValueAction.DropRowMissingValueAction()

        configurator.multiRow              = true

        configurator.keyForConceptPaths    = "${keyVariablePart}Variable"
        configurator.keyForDataType        = "div${keyVariablePart.capitalize()}VariableType"
        configurator.keyForSearchKeywordId = "div${keyVariablePart.capitalize()}VariablePathway"

        configurator.multiRow              = true

        BinningColumnConfigurator binningColumnConfigurator =
                configurator.binningConfigurator
        binningColumnConfigurator.keyForDoBinning       = "binning${keyBinPart.capitalize()}"
        binningColumnConfigurator.keyForManualBinning   = "manualBinning${keyBinPart.capitalize()}"
        binningColumnConfigurator.keyForNumberOfBins    = "numberOfBins${keyBinPart.capitalize()}"
        binningColumnConfigurator.keyForBinDistribution = "binDistribution${keyBinPart.capitalize()}"
        binningColumnConfigurator.keyForBinRanges       = "binRanges${keyBinPart.capitalize()}"
        binningColumnConfigurator.keyForVariableType    = "variableType${keyBinPart.capitalize()}"
    }

}
