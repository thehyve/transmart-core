package jobs

import jobs.steps.*
import jobs.steps.helpers.BinningColumnConfigurator
import jobs.steps.helpers.ColumnConfigurator
import jobs.steps.helpers.OptionalBinningColumnConfigurator
import jobs.steps.helpers.SimpleAddColumnConfigurator
import jobs.table.Table
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.projections.Projection

import static jobs.steps.AbstractDumpStep.DEFAULT_OUTPUT_FILE_NAME

abstract class CategoricalOrBinnedJob extends AbstractLocalRAnalysisJob implements InitializingBean {

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
                temporaryDirectory: temporaryDirectory,
                outputFileName: DEFAULT_OUTPUT_FILE_NAME)

        steps << new RCommandsStep(
                temporaryDirectory: temporaryDirectory,
                scriptsDirectory: scriptsDirectory,
                rStatements: RStatements,
                studyName: studyName,
                params: params,
                extraParams: [inputFileName: DEFAULT_OUTPUT_FILE_NAME])

        steps
    }

    protected void configureConfigurator(OptionalBinningColumnConfigurator configurator,
                                         String keyBinPart,
                                         String keyVariablePart,
                                         String header = null) {
        if (header != null) {
            configurator.header = header
        }
        configurator.projection            = Projection.LOG_INTENSITY_PROJECTION

        configurator.multiRow              = true

        configurator.keyForConceptPaths    = "${keyVariablePart}Variable"
        configurator.keyForDataType        = "div${keyVariablePart.capitalize()}VariableType"
        configurator.keyForSearchKeywordId = "div${keyVariablePart.capitalize()}VariablePathway"

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
