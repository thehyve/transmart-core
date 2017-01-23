package org.transmartproject.batch.highdim.platform

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.clinical.db.objects.Tables

/**
 * Tasklet that issues a query to determine whether a certain platform exists.
 */
@Component
@JobScopeInterfaced
@Slf4j
class PlatformCheckTasklet implements Tasklet {

    public final static String PLATFORM_OBJECT_CTX_KEY = 'platformObject'

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Value("#{jobParameters['PLATFORM']}") // sync AbstractPlatformJobParameters.PLATFORM
    String platformFromParameters

    @Value("#{jobExecutionContext['platform.id']}") // sync PlatformJobContextKeys.PLATFORM
    String platformFromContext

    @Value('#{jobExecution.executionContext}')
    private ExecutionContext jobExecutionContext

    boolean savePlatformInJobContext = true

    private String getPlatform() {
        // try first the job pararameters (annotation loading job)
        // then try to go to the job context (others)
        def result = platformFromParameters ?: platformFromContext
        if (!result) {
            throw new IllegalStateException('Expected platform to be found ' +
                    'either in the job parameters (key ' +
                    "$AbstractPlatformJobSpecification.PLATFORM) or job context " +
                    "(key $PlatformJobContextKeys.PLATFORM).")
        }

        result
    }

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

        Platform platformObject = makePlatformObject(result[0])

        if (log.isInfoEnabled()) {
            log.info("Platform $platform is already present in the database: " +
                    platformObject)
        }

        if (savePlatformInJobContext) {
            jobExecutionContext.put(PLATFORM_OBJECT_CTX_KEY, platformObject)
        }

        contribution.incrementReadCount()
        RepeatStatus.FINISHED
    }

    private Platform makePlatformObject(Map currentData) {
        new Platform(
                id: platform,
                title: currentData['title'],
                organism: currentData['organism'],
                markerType: currentData['marker_type'])
    }
}
