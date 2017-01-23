package org.transmartproject.batch.highdim.concept

import org.springframework.batch.core.Step
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.transmartproject.batch.beans.StepBuildingConfigurationTrait

/**
 * i2b2 spring batch steps configuration
 */
@Configuration
@ComponentScan
class HighDimConceptStepsConfig implements StepBuildingConfigurationTrait {

    @Bean
    Step validatePatientIntersection(Tasklet validatePatientIntersectionTasklet) {
        allowStartStepOf('validatePatientIntersection', validatePatientIntersectionTasklet)
    }

    @Bean
    Step validateHighDimensionalConcepts(Tasklet validateHighDimensionalConceptsTasklet) {
        allowStartStepOf('validateHighDimensionalConcepts', validateHighDimensionalConceptsTasklet)
    }
}
