package jobs

import jobs.steps.AcghRegionDumpDataStep
import jobs.steps.BuildTableResultStep
import jobs.steps.OpenHighDimensionalDataStep
import jobs.steps.ParametersFileStep
import jobs.steps.RCommandsStep
import jobs.steps.SimpleDumpTableResultStep
import jobs.steps.Step
import jobs.steps.helpers.CategoricalColumnConfigurator
import jobs.steps.helpers.HighDimensionColumnConfigurator
import jobs.steps.helpers.SimpleAddColumnConfigurator
import jobs.table.Table
import jobs.table.columns.PrimaryKeyColumn
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import javax.annotation.PostConstruct
import static jobs.steps.AbstractDumpStep.DEFAULT_OUTPUT_FILE_NAME

abstract class AcghAnalysisJob extends AbstractAnalysisJob {

    @Autowired
    HighDimensionResource highDimensionResource

    @Autowired
    SimpleAddColumnConfigurator primaryKeyColumnConfigurator

    @Autowired
    CategoricalColumnConfigurator groupByConfigurator

    @Autowired
    HighDimensionColumnConfigurator highDimensionColumnConfigurator

    @PostConstruct
    void init() {
        primaryKeyColumnConfigurator.column = new PrimaryKeyColumn(header: 'PATIENT_NUM')

        groupByConfigurator.header                    = 'group'
        groupByConfigurator.keyForConceptPaths        = 'groupVariable'
    }

    @Autowired
    Table table

    @Override
    protected List<Step> prepareSteps() {
        List<Step> steps = []

        steps << new ParametersFileStep(
                temporaryDirectory: temporaryDirectory,
                params: params)

        steps << new BuildTableResultStep(
                table:         table,
                configurators: [primaryKeyColumnConfigurator,
                        groupByConfigurator])

        steps << new SimpleDumpTableResultStep(table: table,
                temporaryDirectory: temporaryDirectory,
                outputFileName: 'phenodata.tsv'
        )

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
                extraParams: [inputFileName: DEFAULT_OUTPUT_FILE_NAME])

        steps
    }

    @Override
    protected Step createDumpHighDimensionDataStep(Closure resultsHolder) {
        new AcghRegionDumpDataStep(
                temporaryDirectory: temporaryDirectory,
                resultsHolder: resultsHolder,
                params: params)
    }

}
