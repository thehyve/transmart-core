package org.transmartproject.batch.facts

import org.springframework.batch.core.Step
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemStreamReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.beans.StepBuildingConfigurationTrait

/**
 * Observation facts spring batch steps configuration
 */
@Configuration
@ComponentScan
class ObservationFactStepsConfig implements StepBuildingConfigurationTrait {

    public static final int WRITE_ASSAY_CHUNK_SIZE = 50

    @Bean
    @JobScopeInterfaced
    Tasklet deleteObservationFactTasklet() {
        new DeleteObservationFactTasklet()
    }

    @Bean
    Step deleteObservationFacts(Tasklet deleteObservationFactTasklet) {
        stepOf('deleteObservationFact', deleteObservationFactTasklet)
    }

    @Bean
    Step insertPseudoFacts(ItemStreamReader<ClinicalFactsRowSet> dummyFactGenerator,
                          ObservationFactTableWriter observationFactTableWriter) {
        steps.get('writePseudoFactsStep')
                .chunk(WRITE_ASSAY_CHUNK_SIZE)
                .reader(dummyFactGenerator)
                .writer(observationFactTableWriter)
                .build()
    }

    @Bean
    Step insertHighDimFacts(ItemStreamReader<ClinicalFactsRowSet> dummyFactGenerator,
                           ObservationFactTableWriter observationFactTableWriter) {
        steps.get('writeHighDimFactsStep')
                .chunk(WRITE_ASSAY_CHUNK_SIZE)
                .reader(dummyFactGenerator)
                .writer(observationFactTableWriter)
                .build()
    }

}
