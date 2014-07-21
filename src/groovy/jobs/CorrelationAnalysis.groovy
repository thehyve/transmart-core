package jobs

import jobs.steps.BuildTableResultStep
import jobs.steps.CorrelationAnalysisDumpDataStep
import jobs.steps.ParametersFileStep
import jobs.steps.RCommandsStep
import jobs.steps.Step
import jobs.steps.helpers.GroupNamesHolder
import jobs.steps.helpers.MultiNumericClinicalVariableColumnConfigurator
import jobs.steps.helpers.SimpleAddColumnConfigurator
import jobs.table.Table
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

import static jobs.steps.AbstractDumpStep.DEFAULT_OUTPUT_FILE_NAME


@Component
@Scope('job')
class CorrelationAnalysis extends AbstractAnalysisJob {
    @Autowired
    SimpleAddColumnConfigurator primaryKeyColumnConfigurator

    @Autowired
    MultiNumericClinicalVariableColumnConfigurator columnConfigurator

    @Autowired
    Table table

    GroupNamesHolder holder = new GroupNamesHolder()

    @PostConstruct
    void init() {

        columnConfigurator.header = 'VALUE'
        columnConfigurator.keyForConceptPaths = 'variablesConceptPaths'
        columnConfigurator.groupNamesHolder = holder

    }

    @Override
    protected List<Step> prepareSteps() {

        List<Step> steps = []

        steps << new ParametersFileStep(
                temporaryDirectory: temporaryDirectory,
                params: params)

        steps << new BuildTableResultStep(
                table:         table,
                configurators: [columnConfigurator])

        steps << new CorrelationAnalysisDumpDataStep(
                table:              table,
                temporaryDirectory: temporaryDirectory,
                groupNamesHolder:   holder,
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
            '''source('$pluginDirectory/Correlation/CorrelationLoader.r')''',
            '''Correlation.loader(input.filename='$inputFileName',
                    correlation.by='$correlationBy',
                    correlation.method='$correlationType')'''
        ]
    }

    @Override
    protected getForwardPath() {
        "/correlationAnalysis/correlationAnalysisOutput?jobName=$name"
    }

}
