package org.transmartproject.batch.highdim.mrna.platform

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.transmartproject.batch.AppConfig
import org.transmartproject.batch.db.DbConfig
import org.transmartproject.batch.highdim.platform.PlatformStepsConfig

import javax.annotation.Resource

/**
 * Spring configuration for the mrna annotation (platform) upload job.
 */
@Configuration
@Import([
        AppConfig,
        DbConfig,

        PlatformStepsConfig,
        MrnaPlatformStepsConfig,
])
class MrnaPlatformJobConfig {

    public static final String JOB_NAME = 'mrnaPlatformLoadJob'

    @Autowired
    JobBuilderFactory jobs

    @Resource
    Step deleteGplInfo
    @Resource
    Step insertGplInfo

    @Resource
    Step deleteAnnotations
    @Resource
    Step insertAnnotations

    @Bean
    Job mrnaPlatformLoadJob() {
        jobs.get(JOB_NAME)
                .start(deleteAnnotations)
                .next(deleteGplInfo)
                .next(insertGplInfo)
                .next(insertAnnotations)
                .build()
    }
}

