package org.transmartproject.batch.gwas.analysisdata

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.StepScopeInterfaced
import org.transmartproject.batch.clinical.db.objects.Tables

/**
 * Executes an insert statement for the bio_asy_analysis_gwas_top50 table from
 * the rows in bio_assay_analysis_gwas that have a smaller p_value.
 */
@Component
@StepScopeInterfaced
class UpdateGwasTop500Tasklet implements Tasklet {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate

    @Value("#{currentGwasAnalysisContext.bioAssayAnalysisId}")
    private Long bioAssayAnalysisId

    @Value("#{currentGwasAnalysisContext.metadataEntry.analysisName}")
    private String analysisName

    @Value("#{jobParameters['HG_VERSION']}")
    private String hgVersion

    @Override
    RepeatStatus execute(StepContribution contribution,
                         ChunkContext chunkContext) throws Exception {
        assert analysisName != null
        assert hgVersion != null

        /* postgres would be much faster (10 times) with ORDER BY and LIMIT,
         * but this way it works in both Oracle and PostgreSQL */
        /* distinct is needed because, at least in some versions of transmart
         * there is more than 1 one row for a given (rs id, hg version) pair */
        int affected = jdbcTemplate.update("""
                INSERT INTO ${Tables.BIO_ASY_ANAL_GWAS_TOP500}
                    (bio_assay_analysis_id,
                     analysis,
                     chrom,
                     pos,
                     rsgene,
                     rsid,
                     pvalue,
                     logpvalue,
                     extdata,
                     rnum,
                     intronexon,
                     recombinationrate,
                     regulome)
                SELECT * FROM (
                    SELECT DISTINCT
                        :baa_id,
                        :analysis_name AS analysis,
                        INFO.chrom AS chrom,
                        INFO.pos AS pos,
                        INFO.gene_name AS rsgene,
                        DATA.rs_id AS rsid,
                        DATA.p_value AS pvalue,
                        DATA.log_p_value AS logpvalue,
                        DATA.ext_data AS extdata,
                        row_number() OVER(ORDER BY DATA.p_value ASC, DATA.rs_id ASC) AS rnum,
                        INFO.exon_intron AS intronexon,
                        INFO.recombination_rate AS recombinationrate,
                        INFO.regulome_score AS regulome
                    FROM
                        ${Tables.BIO_ASSAY_ANALYSIS_GWAS} DATA
                        LEFT JOIN ${Tables.RC_SNP_INFO} INFO
                            ON DATA.rs_id = info.rs_id AND (hg_version = :hg_version)
                    WHERE
                        DATA.bio_assay_analysis_id = :baa_id
                ) A
                WHERE rnum <= 500
                """, [
                        hg_version: hgVersion,
                        baa_id: bioAssayAnalysisId,
                        analysis_name: analysisName])

        contribution.incrementWriteCount(affected)

        RepeatStatus.FINISHED
    }
}
