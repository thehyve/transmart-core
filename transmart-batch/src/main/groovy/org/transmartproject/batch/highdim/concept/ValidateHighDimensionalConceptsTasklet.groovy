package org.transmartproject.batch.highdim.concept

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.concept.ConceptTree
import org.transmartproject.batch.concept.ConceptType

/**
 * Check that the high-dimensional nodes for our data, if already existing, have
 * the correct type (and that iof the node needs to be created, the parent is
 * not a numerical or high dim node). The check itself is made in
 * {@link ConceptTree#getOrGenerate}.
 */
@Component
@JobScopeInterfaced
class ValidateHighDimensionalConceptsTasklet implements Tasklet {

    @Autowired
    ConceptTree conceptTree

    @Value("#{jobExecutionContext['gatherCurrentConceptsTasklet.listOfConcepts']}")
    Collection<ConceptPath> conceptPaths

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        conceptPaths.each {
            conceptTree.getOrGenerate(it, ConceptType.HIGH_DIMENSIONAL)
        }

        RepeatStatus.FINISHED
    }
}
