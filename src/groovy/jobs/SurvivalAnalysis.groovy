package jobs

import jobs.steps.*
import jobs.steps.helpers.*
import jobs.table.Column
import jobs.table.MissingValueAction
import jobs.table.Table
import jobs.table.columns.PrimaryKeyColumn
import jobs.table.columns.TransformColumnDecorator
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.projections.Projection

import static jobs.steps.AbstractDumpStep.DEFAULT_OUTPUT_FILE_NAME

/**
 * Created by carlos on 1/20/14.
 */
@Component
@Scope('job')
class SurvivalAnalysis extends AbstractLocalRAnalysisJob implements InitializingBean {

    private static def CENSORING_TRUE = '1'
    private static def CENSORING_FALSE = '0'

    @Autowired
    SimpleAddColumnConfigurator primaryKeyColumnConfigurator

    @Autowired
    NumericColumnConfigurator timeVariableConfigurator

    @Autowired
    @Qualifier('general')
    OptionalBinningColumnConfigurator categoryVariableConfigurator

    @Autowired
    CategoricalColumnConfigurator censoringInnerConfigurator

    CensoringColumnConfigurator censoringVariableConfigurator

    @Autowired
    Table table

    @Override
    void afterPropertiesSet() throws Exception {
        primaryKeyColumnConfigurator.column = new PrimaryKeyColumn(header: 'PATIENT_NUM')

        configureTimeVariableConfigurator()
        configureCategoryVariableConfigurator()
        configureCensoringVariableConfigurator()
    }

    void configureTimeVariableConfigurator() {
        timeVariableConfigurator.header = 'TIME'
        timeVariableConfigurator.setKeys('time')
        timeVariableConfigurator.alwaysClinical = true
    }

    void configureCategoryVariableConfigurator() {
        categoryVariableConfigurator.required = false
        categoryVariableConfigurator.header             = 'CATEGORY'
        categoryVariableConfigurator.projection         = Projection.LOG_INTENSITY_PROJECTION
        categoryVariableConfigurator.multiRow           = true

        categoryVariableConfigurator.setKeys('dependent')
        categoryVariableConfigurator.binningConfigurator.setKeys('')
        categoryVariableConfigurator.keyForConceptPaths = 'categoryVariable'

        def missingValueAction = categoryVariableConfigurator.getConceptPaths() ?
                new MissingValueAction.DropRowMissingValueAction() :
                new MissingValueAction.ConstantReplacementMissingValueAction(replacement: 'STUDY')

        categoryVariableConfigurator.missingValueAction = missingValueAction
        categoryVariableConfigurator.binningConfigurator.missingValueAction = missingValueAction
    }

    void configureCensoringVariableConfigurator() {

        censoringInnerConfigurator.required           = false
        censoringInnerConfigurator.header             = 'CENSOR'
        censoringInnerConfigurator.keyForConceptPaths = 'censoringVariable'

        def noValueDefault = censoringInnerConfigurator.getConceptPaths() ? CENSORING_FALSE : CENSORING_TRUE

        censoringInnerConfigurator.missingValueAction  =
                new MissingValueAction.ConstantReplacementMissingValueAction(replacement: noValueDefault)

        censoringVariableConfigurator = new CensoringColumnConfigurator(innerConfigurator: censoringInnerConfigurator)
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
                        categoryVariableConfigurator,
                ])

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

    @Override
    protected List<String> getRStatements() {
        [
            '''source('$pluginDirectory/Survival/CoxRegressionLoader.r')''',
            '''CoxRegression.loader(
                input.filename      = '$inputFileName')''',
            '''source('$pluginDirectory/Survival/SurvivalCurveLoader.r')''',
            '''SurvivalCurve.loader(
                input.filename      = '$inputFileName',
                concept.time        = '$timeVariable')''',
        ]
    }

    @Override
    protected getForwardPath() {
        "/survivalAnalysis/survivalAnalysisOutput?jobName=$name"
    }

    static class CensoringColumnConfigurator extends ColumnConfigurator {

        ColumnConfigurator innerConfigurator

        @Override
        protected void doAddColumn(Closure<Column> decorateColumn) {
            innerConfigurator.addColumn(compose(decorateColumn, createDecoratorClosure()))
        }

        private Closure<Column> createDecoratorClosure() {
            Closure<Object> function = { value ->
                CENSORING_TRUE
            }
            def decorator = new TransformColumnDecorator(valueFunction: function)
            return { Column originalColumn ->
                decorator.inner = originalColumn
                decorator
            }
        }
    }
}
