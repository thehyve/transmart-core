package jobs

import jobs.steps.BuildTableResultStep
import jobs.steps.OpenHighDimensionalDataStep
import jobs.steps.ParametersFileStep
import jobs.steps.SimpleDumpTableResultStep
import jobs.steps.Step
import jobs.steps.ValueGroupDumpDataStep
import jobs.steps.helpers.CategoricalColumnConfigurator
import jobs.steps.helpers.HighDimensionColumnConfigurator
import jobs.steps.helpers.SimpleAddColumnConfigurator
import jobs.table.Table
import jobs.table.columns.PrimaryKeyColumn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.HighDimensionResource

import javax.annotation.PostConstruct

@Component
@Scope('job')
class FrequencyPlot extends AbstractLocalRAnalysisJob {

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

    @Autowired
    Table regionTable

    @Override
    protected List<Step> prepareSteps() {
        List<Step> steps = []

        println "params is $params"
        println "analysisConstraints is $analysisConstraints"

        steps << new ParametersFileStep(
                temporaryDirectory: temporaryDirectory,
                params: params)

        steps << new BuildTableResultStep(
                table:         table,
                configurators: [primaryKeyColumnConfigurator,
                                groupByConfigurator])

// TODO : To get with aCGH high dimensional

//        def openResultSetStep = new OpenHighDimensionalDataStep(
//                params: params,
//                dataTypeResource: highDimensionResource.getSubResourceForType(analysisConstraints['data_type']),
//                analysisConstraints: analysisConstraints)
//
//        steps << createDumpHighDimensionDataStep { -> openResultSetStep.results }

//        steps << openResultSetStep


        steps << new SimpleDumpTableResultStep(table: table,
                temporaryDirectory: temporaryDirectory,
                outputFileName: 'phenodata.tsv',
                noQuotes: true
        )

        steps
    }

    @Override
    protected Step createDumpHighDimensionDataStep(Closure resultsHolder) {
        new ValueGroupDumpDataStep(
                temporaryDirectory: temporaryDirectory,
                resultsHolder: resultsHolder,
                params: params)
    }

    @Override
    protected List<String> getRStatements() {
        [
                '''source($pluginDirectory/aCGH/acgh-frequency-plot.R)''',
                '''acgh.frequency.plot(column = 'group')'''
        ]
    }

    @Override
    protected getForwardPath() {
        return null
    }
}
