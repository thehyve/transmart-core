package jobs

import org.transmartproject.core.dataquery.TabularResult

class KMeansClustering extends AnalysisJob {

    @Override
    protected void runAnalysis() {
        updateStatus('Running KMeans analysis')

        String source = 'source(\'$pluginDirectory/Heatmap/KMeansHeatmap.R\')'

        // TODO What about clusters.number = 2, probes.aggregate = false?
        String createHeatmap = '''KMeansHeatmap.loader(
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
                        if (row[index] != null) {
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
        updateStatus('Completed', "/RKMeans/heatmapOut?jobName=${name}")
    }
}
