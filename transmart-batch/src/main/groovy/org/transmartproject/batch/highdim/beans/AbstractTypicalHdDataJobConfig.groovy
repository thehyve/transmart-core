package org.transmartproject.batch.highdim.beans

import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.transmartproject.batch.AppConfig
import org.transmartproject.batch.concept.ConceptStepsConfig
import org.transmartproject.batch.db.DbConfig
import org.transmartproject.batch.facts.ObservationFactStepsConfig
import org.transmartproject.batch.highdim.assays.AssayStepsConfig
import org.transmartproject.batch.highdim.concept.HighDimConceptStepsConfig
import org.transmartproject.batch.highdim.platform.PlatformStepsConfig
import org.transmartproject.batch.patient.PatientStepsConfig

import javax.annotation.Resource

/**
 * Typical spring context for HD data loading job.
 */
@Configuration
@Import([
        AppConfig,
        DbConfig,

        PatientStepsConfig,
        ConceptStepsConfig,
        HighDimConceptStepsConfig,
        ObservationFactStepsConfig,
        PlatformStepsConfig,
        AssayStepsConfig,
])
abstract class AbstractTypicalHdDataJobConfig {

    @Autowired
    JobBuilderFactory jobs

    @Resource
    Step gatherCurrentPatients

    @Resource
    Step gatherCurrentConceptCodes
    @Resource
    Step gatherCurrentTreeNodes
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
    Step insertHighDimFacts

    @Resource
    Step deleteObservationFacts

    @Resource
    Step failIfPlatformNotFound

    @Resource
    Step readMappingFile
    @Resource
    Step deleteCurrentAssays
    @Resource
    Step insertAssays

    abstract Step getLoadAnnotationMappings()

    abstract Step getPartitionDataTable()

    abstract Step getFirstPass()

    abstract Step getDeleteHdData()

    abstract Step getSecondPass()

    @Bean
    Flow typicalHdDataFlow() {
        new FlowBuilder<SimpleFlow>('typicalHdDataFlow')
                .start(readMappingFile)
                .next(failIfPlatformNotFound)
                .next(gatherCurrentConceptCodes)
                .next(gatherCurrentTreeNodes)
                .next(validateTopNodePreexistence)
                .next(validateHighDimensionalConcepts)
                .next(gatherCurrentPatients)
                .next(validatePatientIntersection)
                .next(loadAnnotationMappings)
                .next(firstPass)

                .next(deleteHdData)
                .next(deleteCurrentAssays)

                // FIXME: these lines do not only delete observations for
                // highdim data, but also for clinical data.
                // Deleting observations prior to inserting new observations
                // is meant to support reloading of studies.
                // A proper backout script should be written that deletes
                // a selected subset of data for a study.
                //
                //.next(deleteConceptCounts)
                //.next(deleteObservationFacts)

                .next(insertConcepts)
                .next(insertConceptCounts)
                .next(insertAssays)
                .next(insertHighDimFacts)
                .next(partitionDataTable)
                .next(secondPass)
                .build()
    }
}
