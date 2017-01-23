package org.transmartproject.batch.highdim.cnv.data

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.transmartproject.batch.highdim.beans.AbstractTypicalHdDataJobConfig
import org.transmartproject.batch.highdim.platform.chrregion.ChromosomalRegionStepsConfig

import javax.annotation.Resource

/**
 * Spring context for Cnv data loading job.
 */
@Configuration
@Import([
        ChromosomalRegionStepsConfig,
        CnvDataStepsConfig,
])
class CnvDataJobConfig extends AbstractTypicalHdDataJobConfig {

    public static final String JOB_NAME = 'cnvDataJob'

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
    Job cnvDataJob() {
        jobs.get(JOB_NAME)
                .start(typicalHdDataFlow())
                .end()
                .build()
    }
}
