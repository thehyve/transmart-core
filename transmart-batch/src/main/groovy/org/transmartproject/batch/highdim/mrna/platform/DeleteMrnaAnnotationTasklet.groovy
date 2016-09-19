package org.transmartproject.batch.highdim.mrna.platform

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
 * Removes data related to a specific mrna platform from de_mrna_annotation
 */
@Component
@JobScopeInterfaced
class DeleteMrnaAnnotationTasklet implements Tasklet {

    @Autowired
    Platform platform

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        int i = jdbcTemplate.update("""
                DELETE FROM ${Tables.MRNA_ANNOTATION}
                WHERE gpl_id = :gpl_info
        """, [gpl_info: platform.id])

        contribution.incrementWriteCount(i /* 1 */)
    }
}
