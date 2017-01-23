package org.transmartproject.batch.highdim.metabolomics.platform

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.highdim.platform.Platform

/**
 * Removes data related to a specific metabolomics platform
 */
@Component
@JobScopeInterfaced
@Slf4j
class DeleteMetabolomicsAnnotationTasklet implements Tasklet {

    @Autowired
    Platform platform

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        int i = jdbcTemplate.update("""
                DELETE
                FROM ${Tables.METAB_ANNOT_SUB} ANS
                WHERE EXISTS(
                    SELECT * FROM
                        ${Tables.METAB_SUB_PATH} S,
                        ${Tables.METAB_ANNOTATION} A
                    WHERE S.id = ANS.sub_pathway_id AND
                        A.id = ANS.metabolite_id AND
                        A.gpl_id = :gpl_info OR S.gpl_id = :gpl_info)
        """, [gpl_info: platform.id])

        log.info("Deleted $i rows from ${Tables.METAB_ANNOT_SUB}")

        int j = deleteFromTable(Tables.METAB_SUB_PATH)
        int k = deleteFromTable(Tables.METAB_SUPER_PATH)
        int l = deleteFromTable(Tables.METAB_ANNOTATION)

        contribution.incrementWriteCount(i + j + k + l)
    }

    private int deleteFromTable(String table) {
        int i = jdbcTemplate.update("""
                DELETE FROM $table
                WHERE gpl_id = :gpl_info
        """, [gpl_info: platform.id])

        log.info("Deleted $i rows from $table")

        i
    }
}
