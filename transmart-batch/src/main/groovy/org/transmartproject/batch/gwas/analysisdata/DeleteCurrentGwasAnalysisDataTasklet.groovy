package org.transmartproject.batch.gwas.analysisdata

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.gwas.metadata.CurrentGwasAnalysisContext

/**
 * Delete data for a certain analysis from the bio_assay_analysis_gwas table.
 * It has to be step scoped, since the bioAsssayAnalysis will change as we
 * iterate through the analyses.
 */
@Component
@StepScope
@Slf4j
class DeleteCurrentGwasAnalysisDataTasklet implements Tasklet {

    @Autowired
    private CurrentGwasAnalysisContext currentGwasAnalysisContext

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate

    @Override
    RepeatStatus execute(StepContribution contribution,
                         ChunkContext chunkContext) throws Exception {
        assert currentGwasAnalysisContext.bioAssayAnalysisId != null
        def id = currentGwasAnalysisContext.bioAssayAnalysisId

        def affected
        affected = jdbcTemplate.update """
            DELETE FROM ${Tables.BIO_ASY_ANAL_GWAS_TOP500}
            WHERE bio_assay_analysis_id = :id
        """, [id: id]
        log."${affected ? 'info' : 'debug'}"("Deleted $affected rows from " +
                "${Tables.BIO_ASY_ANAL_GWAS_TOP500} for " +
                "bio_assay_analysis_id = $id")
        contribution.incrementWriteCount(affected)

        affected = jdbcTemplate.update """
            DELETE FROM ${Tables.BIO_ASSAY_ANALYSIS_GWAS}
            WHERE bio_assay_analysis_id = :id
        """, [id: id]

        log."${affected ? 'info' : 'debug'}"("Deleted $affected rows from " +
                "${Tables.BIO_ASSAY_ANALYSIS_GWAS} for " +
                "bio_assay_analysis_id = $id")
        contribution.incrementWriteCount(affected)
    }
}
