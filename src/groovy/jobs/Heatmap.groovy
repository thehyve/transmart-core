package jobs

import jobs.steps.Step
import jobs.steps.ValueGroupDumpDataStep
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope('job')
class Heatmap extends HighDimensionalOnlyJob {

    @Override
    protected Step createDumpHighDimensionDataStep(Closure resultsHolder) {
        new ValueGroupDumpDataStep(
                temporaryDirectory: temporaryDirectory,
                resultsHolder: resultsHolder,
                params: params)
    }

    @Override
    protected List<String> getRStatements() {
        String source = 'source(\'$pluginDirectory/Heatmap/HeatmapLoader.R\')'

        String createHeatmap = '''Heatmap.loader(
                            input.filename = '$inputFileName',
                            aggregate.probes = '$divIndependentVariableprobesAggregation' == 'true',
                            ${ txtMaxDrawNumber ? ", maxDrawNumber  = as.integer('$txtMaxDrawNumber')" : ''}
                            )'''

        [ source, createHeatmap ]
    }

    @Override
    protected getForwardPath() {
        "/RHeatmap/heatmapOut?jobName=${name}"
    }

}
