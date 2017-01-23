package org.transmartproject.batch.facts

import org.springframework.batch.core.Step
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.ItemWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.beans.StepBuildingConfigurationTrait
import org.transmartproject.batch.highdim.assays.AssayMappingsRowStore

/**
 * Observation facts spring batch steps configuration
 */
@Configuration
@ComponentScan
class ObservationFactStepsConfig implements StepBuildingConfigurationTrait {

    public static final int WRITE_ASSAY_CHUNK_SIZE = 50

    @Bean
    @JobScopeInterfaced
    Tasklet deleteObservationFactTasklet(AssayMappingsRowStore mappings) {
        new DeleteObservationFactTasklet(
                highDim: true,
                basePaths: mappings.allConceptPaths)
    }

    @Bean
    Step deleteObservationFacts(Tasklet deleteObservationFactTasklet) {
        stepOf('deleteObservationFact', deleteObservationFactTasklet)
    }

    @Bean
    Step insertPseudoFacts(ItemStreamReader<ClinicalFactsRowSet> dummyFactGenerator,
                          ItemWriter<ClinicalFactsRowSet> observationFactTableWriter) {
        steps.get('writePseudoFactsStep')
                .chunk(WRITE_ASSAY_CHUNK_SIZE)
                .reader(dummyFactGenerator)
                .writer(observationFactTableWriter)
                .build()
    }

}
