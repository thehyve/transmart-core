package jobs

import jobs.steps.BioMarkerDumpDataStep
import jobs.steps.Step
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope('job')
class MarkerSelection extends HighDimensionalOnlyJob {

    @Override
    protected Step createDumpHighDimensionDataStep(Closure resultsHolder) {
        new BioMarkerDumpDataStep(
                temporaryDirectory: temporaryDirectory,
                resultsHolder: resultsHolder,
                params: params)
    }

    @Override
    protected List<String> getRStatements() {
        // set path to markerselection processor
        String sourceMarkerSelection = 'source(\'$pluginDirectory/MarkerSelection/MarkerSelection.R\')'
        // call for analysis for marker selection
        // TODO number of permutations is not set? numberOfPermutations = as.integer(\'$txtNumberOfPermutations\'),
        String markerSelectionLoad = '''MS.loader(
                            input.filename = \'$inputFileName\',
                            numberOfMarkers = as.integer(\'$txtNumberOfMarkers\'))'''
        // set path to heatmap png file generator
        String sourceHeatmap = 'source(\'$pluginDirectory/Heatmap/HeatmapLoader.R\')'
        // generate the actual heatmap png picture
        String createHeatmap = '''Heatmap.loader(
                            input.filename   = \'heatmapdata\',
                            meltData         = FALSE,
                            aggregate.probes = '$divIndependentVariableprobesAggregation' == 'true'
                            )'''

        [ sourceMarkerSelection, markerSelectionLoad, sourceHeatmap, createHeatmap ]
    }

    @Override
    final String getForwardPath() {
        "/markerSelection/markerSelectionOut?jobName=${name}"
    }
}
