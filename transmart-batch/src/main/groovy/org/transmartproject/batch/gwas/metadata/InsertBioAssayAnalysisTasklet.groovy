package org.transmartproject.batch.gwas.metadata

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.batch.biodata.BioAssayAnalysisDAO

/**
 * Inserts or updates data in the tables bio_assay_analysis and
 * bio_assay_analysis_ext.
 */
@Component
class InsertBioAssayAnalysisTasklet implements Tasklet {

    @Autowired
    private CurrentGwasAnalysisContext currentGwasAnalysisContext

    @Autowired
    private BioAssayAnalysisDAO bioAssayAnalysisDAO

    @Override
    RepeatStatus execute(StepContribution contribution,
                         ChunkContext chunkContext) throws Exception {

        GwasMetadataEntry entry = currentGwasAnalysisContext.metadataEntry

        def bioAssayAnalysisId = bioAssayAnalysisDAO
                .insertOrUpdateBioAssayAnalysis(entry.properties)

        def bioAssayAnalysisExtId = bioAssayAnalysisDAO
                .insertOrUpdateBioAssayAnalysisExt(
                bioAssayAnalysisId, entry.properties)

        contribution.incrementWriteCount(1)

        // currentGwasAnalysisContext should be registered as a listener for the step
        currentGwasAnalysisContext.updateIds(
                bioAssayAnalysisId, bioAssayAnalysisExtId)

        RepeatStatus.FINISHED
    }
}
