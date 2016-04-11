package org.transmartproject.batch.highdim.platform.chrregion

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
 * Spring configuration for the chromosomal region data load job.
 */
@Configuration
@Import([
        AppConfig,
        DbConfig,

        PlatformStepsConfig,
        ChromosomalRegionStepsConfig,
])
class ChromosomalRegionJobConfig {

    public static final String JOB_NAME = 'chromosomalRegionLoadJob'

    @Autowired
    JobBuilderFactory jobs

    @Resource
    Step deleteGplInfo
    @Resource
    Step insertGplInfo

    @Resource
    Step deleteChromosomalRegions
    @Resource
    Step insertChromosomalRegions

    @Bean
    Job chromosomalRegionLoadJob() {
        jobs.get(JOB_NAME)
                .start(deleteChromosomalRegions)
                .next(deleteGplInfo)
                .next(insertGplInfo)
                .next(insertChromosomalRegions)
                .build()
    }
}
