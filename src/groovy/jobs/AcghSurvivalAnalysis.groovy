package jobs

import jobs.steps.*
import jobs.steps.helpers.CategoricalColumnConfigurator
import jobs.steps.helpers.NumericColumnConfigurator
import jobs.steps.helpers.SimpleAddColumnConfigurator
import jobs.table.MissingValueAction
import jobs.table.Table
import jobs.table.columns.PrimaryKeyColumn
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import static jobs.steps.AbstractDumpStep.DEFAULT_OUTPUT_FILE_NAME

@Component
@Scope('job')
class AcghSurvivalAnalysis extends AbstractLocalRAnalysisJob implements InitializingBean {

    private static def CENSORING_TRUE = '1'
    private static def CENSORING_FALSE = '0'

    @Autowired
    HighDimensionResource highDimensionResource

    @Autowired
    SimpleAddColumnConfigurator primaryKeyColumnConfigurator

    @Autowired
    NumericColumnConfigurator timeVariableConfigurator

    @Autowired
    CategoricalColumnConfigurator censoringInnerConfigurator

    SurvivalAnalysis.CensoringColumnConfigurator censoringVariableConfigurator

    @Autowired
    Table table

    @Override
    void afterPropertiesSet() throws Exception {
        primaryKeyColumnConfigurator.column = new PrimaryKeyColumn(header: 'PATIENT_NUM')

        configureTimeVariableConfigurator()
        configureCensoringVariableConfigurator()
    }

    void configureTimeVariableConfigurator() {
        timeVariableConfigurator.header = 'TIME'
        timeVariableConfigurator.setKeys('time')
        timeVariableConfigurator.alwaysClinical = true
    }

    void configureCensoringVariableConfigurator() {

        censoringInnerConfigurator.required           = false
        censoringInnerConfigurator.header             = 'CENSOR'
        censoringInnerConfigurator.keyForConceptPaths = 'censoringVariable'

        def noValueDefault = censoringInnerConfigurator.getConceptPaths() ? CENSORING_FALSE : CENSORING_TRUE

        censoringInnerConfigurator.missingValueAction  =
                new MissingValueAction.ConstantReplacementMissingValueAction(replacement: noValueDefault)

        censoringVariableConfigurator = new SurvivalAnalysis.CensoringColumnConfigurator(innerConfigurator: censoringInnerConfigurator)
    }

    protected List<Step> prepareSteps() {
        List<Step> steps = []

        steps << new ParametersFileStep(
                temporaryDirectory: temporaryDirectory,
                params: params)

        steps << new BuildTableResultStep(
                table:         table,
                configurators: [primaryKeyColumnConfigurator,
                        timeVariableConfigurator,
                        censoringVariableConfigurator,
                ])

        steps << new MultiRowAsGroupDumpTableResultsStep(
                table: table,
                temporaryDirectory: temporaryDirectory,
                outputFileName: 'phenodata.tsv')

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

    @Override
    protected List<String> getRStatements() {
        [
                '''source('$pluginDirectory/aCGH/acgh-survival-test.R')''',
                '''acgh.survival.test(survival               = 'TIME',
                                      status                 = 'CENSOR',
                                      number.of.permutations = 10000,
                                      test.aberrations       = $aberrationType)''',
                '''source('$pluginDirectory/aCGH/acgh-plot-survival.R')''',
                '''acgh.plot.survival(survival             = 'TIME',
                                      status               = 'CENSOR',
                                      aberrations          = $aberrationType,
                                      confidence.intervals = '$confidenceIntervals')'''
        ]
    }

    @Override
    protected getForwardPath() {
        return "/aCGHSurvivalAnalysis/aCGHSurvivalAnalysisOutput?jobName=${name}"
    }

}
