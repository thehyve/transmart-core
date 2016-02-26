package org.transmartproject.batch.highdim.acgh.data

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.transmartproject.batch.highdim.beans.AbstractTypicalHdDataJobConfig
import org.transmartproject.batch.highdim.platform.chrregion.ChromosomalRegionStepsConfig

import javax.annotation.Resource

/**
 * Spring context for Acgh data loading job.
 */
@Configuration
@Import([
        ChromosomalRegionStepsConfig,
        AcghDataStepsConfig,
])
class AcghDataJobConfig extends AbstractTypicalHdDataJobConfig {

    public static final String JOB_NAME = 'acghDataJob'

    @Resource
    Step loadAnnotationMappings

    @Resource
    Step partitionDataTable
    @Resource
    Step firstPass
    @Resource
    Step deleteHdData
    @Resource
    Step secondPass

    @Bean
    Job acghDataJob() {
        jobs.get(JOB_NAME)
                .start(typicalHdDataFlow())
                .end()
                .build()
    }
}
