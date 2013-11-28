package jobs

import au.com.bytecode.opencsv.CSVWriter
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection

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

    //TODO: Move to abstract Job class and extract writing of the header and row
    @Override
    protected void writeData(Map<String, TabularResult> results) {
        // log.info "study accessions:${i2b2ExportHelperService.findStudyAccessions(params.result_instance_id1)}"
        withDefaultCsvWriter(results) { csvWriter ->
            csvWriter.writeNext(['PATIENT.ID','VALUE','PROBE.ID','GENE_SYMBOL','SUBSET'] as String[])
            results[AnalysisJob.SUBSET1].rows.each { row ->
                row.assayIndexMap.each { assay, index ->
                    csvWriter.writeNext(
                            ['S1_'+assay.assay.patientInTrialId, row.data[index], row.probe, row.geneSymbol, "subset1_${assay.assay.trialName}"] as String[]
                    )
                }
            }
            results[AnalysisJob.SUBSET2].rows.each { row ->
                row.assayIndexMap.each { assay, index ->
                    csvWriter.writeNext(
                            ['S2_'+assay.assay.patientInTrialId, row.data[index], row.probe, row.geneSymbol, "subset2_${assay.assay.trialName}"] as String[]
                    )
                }
            }
        }
    }

    @Override
    protected Map<String, TabularResult> fetchResults() {
        updateStatus('Gathering Data')

        HighDimensionDataTypeResource dataType = highDimensionResource.getSubResourceForType(
                jobDataMap.divIndependentVariableType.toLowerCase()
        )

        List<AssayConstraint> assayConstraints = [
                dataType.createAssayConstraint(
                        AssayConstraint.ONTOLOGY_TERM_CONSTRAINT, concept_key: '\\\\Public Studies' + jobDataMap.variablesConceptPaths
                )
        ]
        assayConstraints.add(
                dataType.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT, result_instance_id: jobDataMap["result_instance_id1"]
                )
        )

        List<DataConstraint> dataConstraints = [
                dataType.createDataConstraint(
                        [keyword_ids: [jobDataMap.divIndependentVariablePathway]], DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT
                )
        ]

        Projection projection = dataType.createProjection([:], 'default_real_projection')

        def set1 = dataType.retrieveData(assayConstraints, dataConstraints, projection)


        HighDimensionDataTypeResource dataType2 = highDimensionResource.getSubResourceForType(
                jobDataMap.divIndependentVariableType.toLowerCase()
        )

        List<AssayConstraint> assayConstraints2 = [
                dataType2.createAssayConstraint(
                        AssayConstraint.ONTOLOGY_TERM_CONSTRAINT, concept_key: '\\\\Public Studies' + jobDataMap.variablesConceptPaths
                )
        ]
        assayConstraints2.add(
                dataType2.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT, result_instance_id: jobDataMap["result_instance_id2"]
                )
        )

        List<DataConstraint> dataConstraints2 = [
                dataType.createDataConstraint(
                        [keyword_ids: [jobDataMap.divIndependentVariablePathway]], DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT
                )
        ]

        Projection projection2 = dataType2.createProjection([:], 'default_real_projection')

        def set2 = dataType2.retrieveData(assayConstraints2, dataConstraints2, projection2)

        return [(AnalysisJob.SUBSET1):set1, (AnalysisJob.SUBSET2):set2]

    }

    @Override
    protected void renderOutput() {
        updateStatus('Completed', "/markerSelection/markerSelectionOut?jobName=${name}")
    }

    private HighDimensionResource getHighDimensionResource() {
        jobDataMap.grailsApplication.mainContext.getBean HighDimensionResource
    }
}
