package jobs

import org.transmartproject.core.dataquery.TabularResult

class MarkerSelection extends AnalysisJob {

    @Override
    protected void runAnalysis() {
        updateStatus('Running marker selection analysis')
        // set path to markerselection processor
        String sourceMarkerSelection = 'source(\'$pluginDirectory/MarkerSelection/MarkerSelection.R\')'
        // call for analysis for marker selection
        // TODO number of permutations is not set? numberOfPermutations = as.integer(\'$txtNumberOfPermutations\'),
        String markerSelectionLoad = '''MS.loader(
                            input.filename = \'outputfile\',
                            numberOfMarkers = as.integer(\'$txtNumberOfMarkers\'))'''
        // set path to heatmap png file generator
        String sourceHeatmap = 'source(\'$pluginDirectory/Heatmap/HeatmapLoader.R\')'
        // generate the actual heatmap png picture
        String createHeatmap = '''Heatmap.loader(
                            input.filename = \'heatmapdata\',
                            meltData       = FALSE,
                            imageWidth     = as.integer(\'$txtImageWidth\'),
                            imageHeight    = as.integer(\'$txtImageHeight\'),
                            pointsize      = as.integer(\'$txtImagePointsize\'))'''

        runRCommandList([sourceMarkerSelection, markerSelectionLoad, sourceHeatmap, createHeatmap])
    }

    @Override
    protected void writeData(Map<String, TabularResult> results) {
        withDefaultCsvWriter(results) { csvWriter ->
            csvWriter.writeNext(['PATIENT.ID','VALUE','PROBE.ID','GENE_SYMBOL','SUBSET'] as String[])

            [AnalysisJob.SUBSET1, AnalysisJob.SUBSET2].each { subset ->
                results[subset].rows.each { row ->
                    row.assayIndexMap.each { assay, index ->
                        csvWriter.writeNext(
                                [
                                        "${AnalysisJob.SHORT_NAME[subset]}_${assay.patientInTrialId}",
                                        row[index],
                                        row.probe,
                                        row.geneSymbol,
                                        assay.trialName
                                ] as String[]
                        )
                    }
                }
            }
        }
    }

    @Override
    protected Map<String, TabularResult> fetchResults() {
        updateStatus('Gathering Data')
        [
                (AnalysisJob.SUBSET1) : fetchSubset(AnalysisJob.RESULT_INSTANCE_ID1),
                (AnalysisJob.SUBSET2) : fetchSubset(AnalysisJob.RESULT_INSTANCE_ID2)
        ]
    }

    @Override
    protected void renderOutput() {
        updateStatus('Completed', "/markerSelection/markerSelectionOut?jobName=${name}")
    }
}
