package org.transmartproject.batch.highdim.mirna.data

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.transmartproject.batch.highdim.beans.AbstractTypicalHdDataJobConfig
import org.transmartproject.batch.highdim.mirna.platform.MirnaPlatformStepsConfig

import javax.annotation.Resource

/**
 * Spring context for miRNA data loading job.
 */
@Configuration
@Import([
        MirnaPlatformStepsConfig,
        MirnaDataStepsConfig,
])
class MirnaDataJobConfig extends AbstractTypicalHdDataJobConfig {

    public static final String JOB_NAME = 'mirnaDataLoadJob'

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
    Job mirnaDataLoadJob() {
        jobs.get(JOB_NAME)
                .start(typicalHdDataFlow())
                .end()
                .build()
    }
}
