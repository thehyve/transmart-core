package jobs

import au.com.bytecode.opencsv.CSVWriter
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection

class Heatmap extends AnalysisJob {

    @Override
    protected void runAnalysis() {
        updateStatus('Running Analysis')

        String source = 'source(\'$pluginDirectory/Heatmap/HeatmapLoader.R\')'

        String createHeatmap = '''Heatmap.loader(
                            input.filename = \'outputfile\',
                            imageWidth     = as.integer(\'$txtImageWidth\'),
                            imageHeight    = as.integer(\'$txtImageHeight\'),
                            pointsize      = as.integer(\'$txtImagePointsize\'),
                            maxDrawNumber  = as.integer(\'$txtMaxDrawNumber\'))'''

        runRCommandList([source, createHeatmap])
    }

    //TODO: Move to abstract Job class and extract writing of the header and row
    @Override
    protected void writeData(TabularResult results) {
        try {
            File output = new File(temporaryDirectory, 'outputfile')
            output.createNewFile()
            output.withWriter {
                CSVWriter writer = new CSVWriter(it, '\t' as char)

                writer.writeNext(['PATIENT_NUM', 'VALUE', 'GROUP'] as String[])

                results.rows.each { row ->
                    row.assayIndexMap.each { assay, index ->
                        // TODO Handle subsets properly
                        writer.writeNext(
                                ['S1_'+assay.assay.patientInTrialId, row.data[index], "${row.probe}_${row.geneSymbol}"] as String[]
                        )
                    }
                }
            }
        } finally {
            results.close()
        }
    }

    @Override
    protected TabularResult fetchResults() {
        updateStatus('Gathering Data')

        HighDimensionDataTypeResource dataType = highDimensionResource.getSubResourceForType(
                jobDataMap.divIndependentVariableType.toLowerCase()
        )

        List<AssayConstraint> assayConstraints = [
                dataType.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT, result_instance_id: jobDataMap["result_instance_id1"]
                )
        ]
        assayConstraints.add(
                dataType.createAssayConstraint(
                        AssayConstraint.ONTOLOGY_TERM_CONSTRAINT, concept_key: '\\\\Public Studies' + jobDataMap.variablesConceptPaths
                )
        )

        List<DataConstraint> dataConstraints = [
                dataType.createDataConstraint(
                        [keyword_ids: [jobDataMap.divIndependentVariablePathway]], DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT
                )
        ]

        Projection projection = dataType.createProjection([:], 'default_real_projection')

        dataType.retrieveData(assayConstraints, dataConstraints, projection)
    }

    protected void renderOutput() {
        updateStatus('Completed', "/RHeatmap/heatmapOut?jobName=${name}")
    }

    private HighDimensionResource getHighDimensionResource() {
        jobDataMap.grailsApplication.mainContext.getBean HighDimensionResource
    }
}
