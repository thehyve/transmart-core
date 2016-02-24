package org.transmartproject.batch.highdim.rnaseq.data

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.transmartproject.batch.BatchConfig
import org.transmartproject.batch.concept.ConceptStepsConfig
import org.transmartproject.batch.db.DbConfig
import org.transmartproject.batch.facts.ObservationFactStepsConfig
import org.transmartproject.batch.highdim.assays.AssayStepsConfig
import org.transmartproject.batch.highdim.concept.HighDimConceptStepsConfig
import org.transmartproject.batch.highdim.platform.PlatformStepsConfig
import org.transmartproject.batch.highdim.platform.chrregion.ChromosomalRegionStepsConfig
import org.transmartproject.batch.patient.PatientStepsConfig

import javax.annotation.Resource

/**
 * Spring context for RNASeq data loading job.
 */
@Configuration
@Import([
        BatchConfig,
        DbConfig,

        PatientStepsConfig,
        ConceptStepsConfig,
        HighDimConceptStepsConfig,
        ObservationFactStepsConfig,
        PlatformStepsConfig,
        ChromosomalRegionStepsConfig,
        AssayStepsConfig,
        RnaSeqDataStepsConfig,
])
class RnaSeqDataJobConfig {

    public static final String JOB_NAME = 'rnaSeqDataLoad'

    @Autowired
    JobBuilderFactory jobs

    @Resource
    Step gatherCurrentPatients

    @Resource
    Step gatherCurrentConcepts
    @Resource
    Step validateTopNodePreexistence
    @Resource
    Step deleteConceptCounts
    @Resource
    Step insertConcepts
    @Resource
    Step insertConceptCounts

    @Resource
    Step validateHighDimensionalConcepts
    @Resource
    Step validatePatientIntersection
    @Resource
    Step writePseudoFacts

    @Resource
    Step deleteObservationFact

    @Resource
    Step ensureThePlatformExists

    @Resource
    Step loadAnnotationMappings

    @Resource
    Step readMappingFile
    @Resource
    Step deleteCurrentAssays
    @Resource
    Step writeAssays

    @Resource
    Step partitionTable
    @Resource
    Step firstPass
    @Resource
    Step deleteRnaSeqData
    @Resource
    Step secondPass

    @Bean
    Job rnaSeqDataLoad() {
        jobs.get(JOB_NAME)
                .start(readMappingFile)
                .next(ensureThePlatformExists)
                .next(gatherCurrentConcepts)
                .next(validateTopNodePreexistence)
                .next(validateHighDimensionalConcepts)
                .next(gatherCurrentPatients)
                .next(validatePatientIntersection)
                .next(loadAnnotationMappings)
                .next(partitionTable)
                .next(firstPass)
                .next(deleteRnaSeqData)
                .next(deleteCurrentAssays)
                .next(deleteConceptCounts)
                .next(deleteObservationFact)
                .next(insertConcepts)
                .next(writeAssays)
                .next(writePseudoFacts)
                .next(insertConceptCounts)
                .next(secondPass)
                .build()
    }
}
