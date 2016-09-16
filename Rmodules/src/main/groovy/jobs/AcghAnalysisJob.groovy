package jobs

import com.recomdata.transmart.util.ZipService
import jobs.steps.*
import jobs.steps.helpers.CategoricalColumnConfigurator
import jobs.steps.helpers.HighDimensionColumnConfigurator
import jobs.steps.helpers.SimpleAddColumnConfigurator
import jobs.table.Table
import jobs.table.columns.PrimaryKeyColumn
import grails.core.GrailsApplication
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

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    ZipService zipService

    @PostConstruct
    void init() {
        primaryKeyColumnConfigurator.column = new PrimaryKeyColumn(header: 'PATIENT_NUM')

        groupByConfigurator.header = 'group'
        groupByConfigurator.keyForConceptPaths = 'groupVariable'
        groupByConfigurator.required = false
    }

    @Autowired
    Table table

    @Override
    protected List<Step> prepareSteps() {
        List<Step> steps = []

        if (groupByConfigurator.getConceptPaths()) {
            steps << new BuildTableResultStep(
                    table: table,
                    configurators: [primaryKeyColumnConfigurator,
                                    groupByConfigurator])

            steps << new SimpleDumpTableResultStep(table: table,
                    temporaryDirectory: temporaryDirectory,
                    outputFileName: 'phenodata.tsv'
            )
        }

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

        steps << new ZipResultsStep(jobName: params.jobName,
                grailsApplication: grailsApplication,
                zipService: zipService)

        steps
    }

    //@Override
    protected Step createDumpHighDimensionDataStep(Closure resultsHolder) {
        new AcghRegionDumpDataStep(
                temporaryDirectory: temporaryDirectory,
                resultsHolder: resultsHolder,
                params: params)
    }

}
