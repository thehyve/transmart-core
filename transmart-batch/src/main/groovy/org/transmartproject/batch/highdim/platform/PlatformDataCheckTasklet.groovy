package org.transmartproject.batch.highdim.platform

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

/**
 * Checks whether there are assays referring to a certain platform.
 */
@Component
@JobScopeInterfaced
@Slf4j
class PlatformDataCheckTasklet implements Tasklet {

    @Autowired
    private Platform platform

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        List result = jdbcTemplate.queryForList """
                SELECT DISTINCT trial_name
                FROM $Tables.SUBJ_SAMPLE_MAP
                WHERE gpl_id = :gpl_id
        """, [gpl_id: platform.id]

        if (result.empty) {
            log.info("No assays for platform ${platform.id}. Good!")
            return
        }

        List<String> studies = result*.trial_name
        studies.size().times { contribution.incrementReadCount() }

        log.error "The following studies have assays for platform $platform: $studies"
    }
}
