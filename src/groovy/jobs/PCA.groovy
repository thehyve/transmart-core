package jobs

import jobs.steps.BioMarkerDumpDataStep
import jobs.steps.Step

class PCA extends AbstractAnalysisJob {

    @Override
    protected Step createDumpHighDimensionDataStep(Closure resultsHolder) {
        new BioMarkerDumpDataStep(
                temporaryDirectory: temporaryDirectory,
                resultsHolder: resultsHolder)
    }


    final List<String> RStatements = [
            '''source('$pluginDirectory/PCA/LoadPCA.R')''',
                '''PCA.loader(input.filename='outputfile')''' ]

    @Override
    protected getForwardPath() {
        "/PCA/pcaOut?jobName=${name}"
    }
}
