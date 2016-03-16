package org.transmartproject.batch.concept

import org.springframework.batch.core.Step
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.beans.StepBuildingConfigurationTrait
import org.transmartproject.batch.concept.oracle.OracleInsertConceptCountsTasklet
import org.transmartproject.batch.concept.postgresql.PostgresInsertConceptCountsTasklet
import org.transmartproject.batch.db.DatabaseImplementationClassPicker
import org.transmartproject.batch.db.DbConfig
import org.transmartproject.batch.secureobject.SecureObjectConfig

/**
 * Concept spring configuration
 */
@Configuration
@ComponentScan
@Import([DbConfig, SecureObjectConfig])
class ConceptStepsConfig implements StepBuildingConfigurationTrait {

    @Autowired
    DatabaseImplementationClassPicker picker

    @Bean
    Step gatherCurrentConcepts(Tasklet gatherCurrentConceptsTasklet) {
        allowStartStepOf('gatherCurrentConcepts', gatherCurrentConceptsTasklet)
    }

    @Bean
    Step validateTopNodePreexistence(Tasklet validateTopNodePreexistenceTasklet) {
        allowStartStepOf('validateTopNodePreexistence', validateTopNodePreexistenceTasklet)
    }

    @Bean
    Step insertConcepts(Tasklet insertConceptsTasklet) {
        allowStartStepOf('insertConcepts', insertConceptsTasklet)
    }

    @Bean
    Step insertConceptCounts(Tasklet insertConceptCountsTasklet) {
        allowStartStepOf('insertConceptCounts', insertConceptCountsTasklet)
    }

    @Bean
    @JobScopeInterfaced
    Tasklet deleteConceptCountsTasklet(
            @Value("#{jobParameters['TOP_NODE']}") ConceptPath topNode) {
        new DeleteConceptCountsTasklet(basePath: topNode)
    }

    @Bean
    Step deleteConceptCounts(Tasklet deleteConceptCountsTasklet) {
        allowStartStepOf('deleteConceptCounts', deleteConceptCountsTasklet)
    }

    @Bean
    @JobScopeInterfaced
    Tasklet insertConceptCountsTasklet(
            @Value("#{jobParameters['TOP_NODE']}") ConceptPath topNode) {
        picker.instantiateCorrectClass(
                OracleInsertConceptCountsTasklet,
                PostgresInsertConceptCountsTasklet).with { InsertConceptCountsTasklet t ->
            t.basePath = topNode
            t
        }
    }
}
