package jobs

import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection

class Heatmap extends AnalysisJob {

    @Override
    protected void runAnalysis() {
        updateStatus('Running Heatmap analysis')

        String source = 'source(\'$pluginDirectory/Heatmap/HeatmapLoader.R\')'

        String createHeatmap = '''Heatmap.loader(
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
            //Write header
            csvWriter.writeNext(['PATIENT_NUM', 'VALUE', 'GROUP'] as String[])

            //Write results. Concatenate both result sets.
            [AnalysisJob.SUBSET1, AnalysisJob.SUBSET2].each { subset ->
                results[subset]?.rows?.each { row ->
                    row.assayIndexMap.each { assay, index ->
                        csvWriter.writeNext(
                                ["${AnalysisJob.SHORT_NAME[subset]}_${assay.assay.patientInTrialId}", row.data[index], "${row.label}"] as String[]
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

    private TabularResult fetchSubset(String subset) {
        if (jobDataMap[subset] == null) {
            return
        }

        HighDimensionDataTypeResource dataType = highDimensionResource.getSubResourceForType(
                jobDataMap.analysisConstraints["data_type"]
        )

        List<DataConstraint> dataConstraints = jobDataMap.analysisConstraints["dataConstraints"].collect { String constraintType, values ->
            if(values) {
                dataType.createDataConstraint(values, constraintType)
            }
        }.grep()

        List<AssayConstraint> assayConstraints = jobDataMap.analysisConstraints["assayConstraints"].collect { String constraintType, values ->
            if(values) {
                dataType.createAssayConstraint(values, constraintType)
            }
        }.grep()

        assayConstraints.add(
                dataType.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT, result_instance_id: jobDataMap[(subset == AnalysisJob.RESULT_INSTANCE_ID1) ? AnalysisJob.RESULT_INSTANCE_ID1 : AnalysisJob.RESULT_INSTANCE_ID2]
                )
        )

        Projection projection = dataType.createProjection([:], jobDataMap.analysisConstraints["projections"][0])
        return dataType.retrieveData(assayConstraints, dataConstraints, projection)
    }

    @Override
    protected void renderOutput() {
        updateStatus('Completed', "/RHeatmap/heatmapOut?jobName=${name}")
    }

    private HighDimensionResource getHighDimensionResource() {
        jobDataMap.grailsApplication.mainContext.getBean HighDimensionResource
    }
}
