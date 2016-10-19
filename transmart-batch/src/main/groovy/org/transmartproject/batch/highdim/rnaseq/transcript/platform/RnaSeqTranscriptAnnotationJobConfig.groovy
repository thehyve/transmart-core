package org.transmartproject.batch.highdim.rnaseq.transcript.platform

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
        RnaSeqTranscriptAnnotationStepsConfig,
])
class RnaSeqTranscriptAnnotationJobConfig {

    public static final String JOB_NAME = 'rnaSeqTranscriptAnnotationsLoadJob'

    @Autowired
    JobBuilderFactory jobs

    @Resource
    Step deleteGplInfo
    @Resource
    Step insertGplInfo

    @Resource
    Step deleteRnaSeqTranscriptAnnotations
    @Resource
    Step insertRnaSeqTranscriptAnnotations

    @Bean
    Job rnaSeqTranscriptAnnotationsLoadJob() {
        jobs.get(JOB_NAME)
                .start(deleteRnaSeqTranscriptAnnotations)
                .next(deleteGplInfo)
                .next(insertGplInfo)
                .next(insertRnaSeqTranscriptAnnotations)
                .build()
    }
}
