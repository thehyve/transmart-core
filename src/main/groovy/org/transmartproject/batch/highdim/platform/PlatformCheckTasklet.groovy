package org.transmartproject.batch.highdim.platform

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.clinical.db.objects.Tables

import javax.annotation.Resource

/**
 * Tasklet that issues a query to determine whether a certain platform exists.
 */
@Component
@JobScopeInterfaced
@Slf4j
class PlatformCheckTasklet implements Tasklet {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Value("#{jobParameters['PLATFORM']}")
    String platform

    @Resource
    Platform platformObject

    @Value('#{jobParameters}')
    Map<String, Object> jobParameters

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        List result = jdbcTemplate.queryForList("""
                SELECT platform, title, organism, marker_type
                FROM ${Tables.GPL_INFO}
                WHERE platform = :platform""",
                [platform: platform])

        if (result.empty) {
            log.info("Platform $platform is not yet in the database")
            return /* finished */
        }

        if (log.isInfoEnabled()) {
            log.info("Platform $platform is already present in the database")
            logDifferences(result[0])
        }

        contribution.incrementReadCount()

        RepeatStatus.FINISHED
    }

    private void logDifferences(Map oldData) {
        Platform oldPlatform = new Platform(
                id: platform,
                title: oldData['title'],
                organism: oldData['organism'],
                markerType: oldData['marker_type'])

        log.info("Old platform: $oldPlatform, new platform: $platformObject")
    }
}
