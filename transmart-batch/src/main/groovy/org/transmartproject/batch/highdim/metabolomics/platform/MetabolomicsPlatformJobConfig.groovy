package org.transmartproject.batch.highdim.metabolomics.platform

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
 * Spring configuration for the metabolimcs platform job.
 */
@Configuration
@Import([
        AppConfig,
        DbConfig,

        PlatformStepsConfig,
        MetabolomicsPlatformStepsConfig,
])
class MetabolomicsPlatformJobConfig {

    public static final String JOB_NAME = 'metabolomicsPlatformLoadJob'

    @Autowired
    JobBuilderFactory jobs

    @Resource
    Step deleteGplInfo
    @Resource
    Step insertGplInfo

    @Resource
    Step deleteMetabolomicsAnnotation
    @Resource
    Step readTheGraph
    @Resource
    Step metabolomicsAssignIds
    @Resource
    Step insertSuperPathways
    @Resource
    Step insertSubPathways
    @Resource
    Step insertBiochemicals
    @Resource
    Step insertBiochemicalsSubPathwaysAssociations

    @Bean
    Job metabolomicsPlatformLoadJob() {
        jobs.get(JOB_NAME)
                .start(deleteMetabolomicsAnnotation)
                .next(deleteGplInfo)
                .next(insertGplInfo)
                .next(readTheGraph)
                .next(metabolomicsAssignIds)
                .next(insertSuperPathways)
                .next(insertSubPathways)
                .next(insertBiochemicals)
                .next(insertBiochemicalsSubPathwaysAssociations)
                .build()
    }
}
