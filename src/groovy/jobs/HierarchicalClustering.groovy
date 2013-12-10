package jobs

import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult

import static jobs.AbstractAnalysisJob.SUBSET1
import static jobs.AbstractAnalysisJob.SUBSET2
import static jobs.AbstractAnalysisJob.SHORT_NAME
import static jobs.AbstractAnalysisJob.RESULT_INSTANCE_IDS

class HierarchicalClustering extends AbstractAnalysisJob {

    @Override
    protected void runAnalysis() {
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

            [SUBSET1, SUBSET2].each { subset ->
                results[subset]?.each { DataRow row ->
                    row.assayIndexMap.each { assay, index -> //XXX: assayIndexMap is private
                        if (row[index] == null) { return }
                        csvWriter.writeNext(
                                ["${SHORT_NAME[subset]}_${assay.patientInTrialId}", row[index], "${row.label}"] as String[]
                        )
                    }
                }
            }
        }
    }
    }

    @Override
    protected Map<String, TabularResult> fetchResults() {
        [
                (SUBSET1) : fetchSubset(RESULT_INSTANCE_IDS[SUBSET1]),
                (SUBSET2) : fetchSubset(RESULT_INSTANCE_IDS[SUBSET2])
        ]
    }

    @Override
    protected getForwardPath() {
        "/RHClust/heatmapOut?jobName=${name}"
    }
}
