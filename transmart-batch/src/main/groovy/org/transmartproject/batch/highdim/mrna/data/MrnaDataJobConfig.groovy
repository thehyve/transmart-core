package org.transmartproject.batch.highdim.mrna.data

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.transmartproject.batch.highdim.beans.AbstractTypicalHdDataJobConfig
import org.transmartproject.batch.highdim.mrna.platform.MrnaPlatformStepsConfig

import javax.annotation.Resource

/**
 * Spring context for mRNA data loading job.
 */
@Configuration
@Import([
        MrnaPlatformStepsConfig,
        MrnaDataStepsConfig,
])
class MrnaDataJobConfig extends AbstractTypicalHdDataJobConfig {

    public static final String JOB_NAME = 'mrnaDataLoadJob'

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
    Job mrnaDataLoadJob() {
        jobs.get(JOB_NAME)
                .start(typicalHdDataFlow())
                .end()
                .build()
    }
}
