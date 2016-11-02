package org.transmartproject.batch.highdim.rnaseq.transcript.data

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.transmartproject.batch.highdim.beans.AbstractTypicalHdDataJobConfig
import org.transmartproject.batch.highdim.rnaseq.transcript.platform.RnaSeqTranscriptAnnotationStepsConfig

import javax.annotation.Resource

/**
 * Spring context for RNASeq data loading job.
 */
@Configuration
@Import([
        RnaSeqTranscriptAnnotationStepsConfig,
        RnaSeqTranscriptDataStepsConfig,
])
class RnaSeqTranscriptDataJobConfig extends AbstractTypicalHdDataJobConfig {

    public static final String JOB_NAME = 'rnaSeqTranscriptDataJob'

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
    Job rnaSeqTranscriptDataJob() {
        jobs.get(JOB_NAME)
                .start(typicalHdDataFlow())
                .end()
                .build()
    }
}
