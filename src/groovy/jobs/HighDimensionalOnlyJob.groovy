package jobs

import jobs.steps.AbstractDumpStep
import jobs.steps.OpenHighDimensionalDataStep
import jobs.steps.ParametersFileStep
import jobs.steps.RCommandsStep
import jobs.steps.Step
import jobs.steps.helpers.NumericColumnConfigurator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.projections.Projection

abstract class HighDimensionalOnlyJob extends AbstractAnalysisJob {

    @Autowired
    HighDimensionResource highDimensionResource

    @Autowired
    ApplicationContext appCtx

    private void configure() {
        def dependentConfigurator   = appCtx.getBean NumericColumnConfigurator
        def independentConfigurator = appCtx.getBean NumericColumnConfigurator

        dependentConfigurator = new NumericColumnConfigurator(
                header: 'X',
                projection: Projection.ZSCORE_PROJECTION,
                keyForConceptPath: 'dependentVariable',
                keyForDataType: 'divDependentVariableType',
                keyForSearchKeywordId: 'divDependentVariablePathway')
        independentConfigurator = new NumericColumnConfigurator(
                header: 'Y',
                projection: Projection.ZSCORE_PROJECTION,
                keyForConceptPath: 'independentVariable',
                keyForDataType: 'divIndependentVariableType',
                keyForSearchKeywordId: 'divIndependentVariablePathway')

        dependentConfigurator.addColumn()
        independentConfigurator.addColumn()
    }

    protected List<Step> prepareSteps() {
        List<Step> steps = []

        steps << new ParametersFileStep(
                temporaryDirectory: temporaryDirectory,
                params: params)

        def openResultSetStep = new OpenHighDimensionalDataStep(
                params: params,
                dataTypeResource: highDimensionResource.getSubResourceForType(analysisConstraints['data_type']),
                analysisConstraints: analysisConstraints)

        steps << openResultSetStep

        steps << createDumpHighDimensionDataStep { -> openResultSetStep.results }

        steps << new RCommandsStep(
                temporaryDirectory: temporaryDirectory,
                scriptsDirectory: scriptsDirectory,
                rStatements: RStatements,
                studyName: studyName,
                params: params,
                extraParams: [inputFileName: AbstractDumpStep.DEFAULT_OUTPUT_FILE_NAME])

        steps
    }

    abstract protected Step createDumpHighDimensionDataStep(Closure resultsHolder)

}
