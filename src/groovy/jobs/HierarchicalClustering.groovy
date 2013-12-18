package jobs

import jobs.steps.Step
import jobs.steps.ValueGroupDumpDataStep

class HierarchicalClustering extends AbstractAnalysisJob {

    @Override
    protected Step createDumpHighDimensionDataStep(Closure resultsHolder) {
        new ValueGroupDumpDataStep(
                temporaryDirectory: temporaryDirectory,
                resultsHolder: resultsHolder,
                params: params)
    }

    @Override
    protected List<String> getRStatements() {
        String source = 'source(\'$pluginDirectory/Heatmap/HClusteredHeatmapLoader.R\')'

        String createHeatmap = '''HClusteredHeatmap.loader(
                            input.filename = \'outputfile\',
                            imageWidth     = as.integer(\'$txtImageWidth\'),
                            imageHeight    = as.integer(\'$txtImageHeight\'),
                            pointsize      = as.integer(\'$txtImagePointsize\'),
                            maxDrawNumber  = as.integer(\'$txtMaxDrawNumber\'))'''

        [ source, createHeatmap ]
    }

    @Override
    protected getForwardPath() {
        "/RHClust/heatmapOut?jobName=${name}"
    }
}
