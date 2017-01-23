package org.transmartproject.batch.highdim.proteomics.data

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.transmartproject.batch.highdim.beans.AbstractTypicalHdDataJobConfig
import org.transmartproject.batch.highdim.proteomics.platform.ProteomicsPlatformStepsConfig

import javax.annotation.Resource

/**
 * Spring context for proteomics data loading job.
 */
@Configuration
@Import([
        ProteomicsPlatformStepsConfig,
        ProteomicsDataStepsConfig,
])
class ProteomicsDataJobConfig extends AbstractTypicalHdDataJobConfig {

    public static final String JOB_NAME = 'proteomicsDataLoadJob'

    @Autowired
    JobBuilderFactory jobs

    @Resource
    Step loadAnnotationMappings

    @Resource
    Step firstPass
    @Resource
    Step deleteHdData
    @Resource
    Step partitionDataTable
    @Resource
    Step secondPass

    @Bean
    Job proteomicsDataLoadJob() {
        jobs.get(JOB_NAME)
                .start(typicalHdDataFlow())
                .end()
                .build()
    }
}
