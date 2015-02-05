package jobs

import jobs.steps.PCADumpDataStep
import jobs.steps.Step
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope('job')
class PCA extends HighDimensionalOnlyJob {

    @Override
    protected Step createDumpHighDimensionDataStep(Closure resultsHolder) {
        new PCADumpDataStep(
                temporaryDirectory: temporaryDirectory,
                resultsHolder: resultsHolder,
                params: params)
    }


    final List<String> RStatements = [
            '''source('$pluginDirectory/PCA/LoadPCA.R')''',
                '''PCA.loader(
                input.filename='$inputFileName',
                aggregate.probes = '$divIndependentVariableprobesAggregation' == 'true',
                calculateZscore = '$calculateZscore'
                )''' ]

    @Override
    protected getForwardPath() {
        "/PCA/pcaOut?jobName=${name}"
    }
}
