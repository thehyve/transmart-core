package org.transmartproject.batch.highdim.metabolomics.data

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.transmartproject.batch.highdim.beans.AbstractTypicalHdDataJobConfig
import org.transmartproject.batch.highdim.metabolomics.platform.MetabolomicsPlatformStepsConfig

import javax.annotation.Resource

/**
 * Spring context for metabolomics data loading job.
 */
@Configuration
@Import([
        MetabolomicsPlatformStepsConfig,
        MetabolomicsDataStepsConfig,
])
class MetabolomicsDataJobConfig extends AbstractTypicalHdDataJobConfig {

    public static final String JOB_NAME = 'metabolomicsDataLoadJob'

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
    Job metabolomicsDataLoadJob() {
        jobs.get(JOB_NAME)
                .start(typicalHdDataFlow())
                .end()
                .build()
    }

}
