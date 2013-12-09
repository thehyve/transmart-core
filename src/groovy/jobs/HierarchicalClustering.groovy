package jobs

import org.transmartproject.core.dataquery.TabularResult

class HierarchicalClustering extends AnalysisJob {

    @Override
    protected void runAnalysis() {
        updateStatus('Running Hierarchical analysis')

        String source = 'source(\'$pluginDirectory/Heatmap/HClusteredHeatmapLoader.R\')'

        String createHeatmap = '''HClusteredHeatmap.loader(
                            input.filename = \'outputfile\',
                            imageWidth     = as.integer(\'$txtImageWidth\'),
                            imageHeight    = as.integer(\'$txtImageHeight\'),
                            pointsize      = as.integer(\'$txtImagePointsize\'),
                            maxDrawNumber  = as.integer(\'$txtMaxDrawNumber\'))'''

        runRCommandList([source, createHeatmap])
    }

    @Override
    protected void writeData(Map<String, TabularResult> results) {
        withDefaultCsvWriter(results) { csvWriter ->
            csvWriter.writeNext(['PATIENT_NUM', 'VALUE', 'GROUP'] as String[])

            [AnalysisJob.SUBSET1, AnalysisJob.SUBSET2].each { subset ->
                results[subset]?.rows?.each { row ->
                    row.assayIndexMap.each { assay, index ->
                        if (row[index] != null) { // value might be 'empty'
                            csvWriter.writeNext(
                                    ["${AnalysisJob.SHORT_NAME[subset]}_${assay.patientInTrialId}", row[index], "${row.label}"] as String[]
                            )
                        }
                    }
                }
            }
        }
    }

    @Override
    protected Map<String, TabularResult> fetchResults() {
        updateStatus('Gathering Data')

        [
                (AnalysisJob.SUBSET1) : fetchSubset(AnalysisJob.RESULT_INSTANCE_IDS[AnalysisJob.SUBSET1]),
                (AnalysisJob.SUBSET2) : fetchSubset(AnalysisJob.RESULT_INSTANCE_IDS[AnalysisJob.SUBSET2])
        ]
    }

    @Override
    protected void renderOutput() {
        updateStatus('Completed', "/RHClust/heatmapOut?jobName=${name}")
    }
}
