package jobs

import jobs.steps.BuildTableResultStep
import jobs.steps.MultiRowAsGroupDumpTableResultsStep
import jobs.steps.ParametersFileStep
import jobs.steps.RCommandsStep
import jobs.steps.Step
import jobs.steps.helpers.BinningColumnConfigurator
import jobs.steps.helpers.CategoricalColumnConfigurator
import jobs.steps.helpers.CategoricalOrBinnedColumnConfigurator
import jobs.steps.helpers.NumericColumnConfigurator
import jobs.steps.helpers.SimpleAddColumnConfigurator
import jobs.table.MissingValueAction
import jobs.table.Table
import jobs.table.columns.PrimaryKeyColumn
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.projections.Projection

/**
 * Created by carlos on 1/20/14.
 */
@Component
@Scope('job')
class SurvivalAnalysis extends AbstractAnalysisJob implements InitializingBean {

    @Autowired
    SimpleAddColumnConfigurator primaryKeyColumnConfigurator

    @Autowired
    NumericColumnConfigurator timeVariableConfigurator

    @Autowired
    CategoricalOrBinnedColumnConfigurator categoryVariableConfigurator

    @Autowired
    CategoricalColumnConfigurator censoringVariableConfigurator

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
        timeVariableConfigurator.columnHeader           = 'TIME'
        timeVariableConfigurator.projection             = Projection.DEFAULT_REAL_PROJECTION
        //timeVariableConfigurator.multiRow               = true
        timeVariableConfigurator.setKeys('time')
        timeVariableConfigurator.alwaysClinical = true
    }

    void configureCategoryVariableConfigurator() {
        categoryVariableConfigurator.required = false
        categoryVariableConfigurator.columnHeader       = 'CATEGORY'
        categoryVariableConfigurator.projection         = Projection.DEFAULT_REAL_PROJECTION
        categoryVariableConfigurator.missingValueAction =
                new MissingValueAction.ConstantReplacementMissingValueAction(replacement: 'STUDY')
        //categoryVariableConfigurator.multiRow           = true

        categoryVariableConfigurator.setKeys('category')
        categoryVariableConfigurator.binningConfigurator.setKeys('')
    }

    void configureCensoringVariableConfigurator() {
        censoringVariableConfigurator.required = false
        censoringVariableConfigurator.columnHeader       = 'CENSOR'
        censoringVariableConfigurator.missingValueAction =
                new MissingValueAction.ConstantReplacementMissingValueAction(replacement: '1')
        //censoringVariableConfigurator.multiRow           = true
        censoringVariableConfigurator.keyForConceptPaths = 'censoringVariable'

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
                        categoryVariableConfigurator,])

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

    @Override
    protected List<String> getRStatements() {
        [
            '''source('$pluginDirectory/Survival/CoxRegressionLoader.r')''',
            '''CoxRegression.loader(
                input.filename      = 'outputfile')''',
            '''source('$pluginDirectory/Survival/SurvivalCurveLoader.r')''',
            '''SurvivalCurve.loader(
                input.filename      = 'outputfile',
                concept.time        = '$timeVariable')''',
        ]
    }

    @Override
    protected getForwardPath() {
        "/survivalAnalysis/survivalAnalysisOut?jobName=$name"
    }
}
