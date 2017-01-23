package org.transmartproject.batch.concept

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced

/**
 * Inserts concepts (from the ConceptTree) that are new
 */
@Component
@JobScopeInterfaced
@Slf4j
class InsertConceptsTasklet implements Tasklet {

    @Autowired
    InsertConceptsService insertConceptsService

    @Autowired
    ConceptTree conceptTree

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        //gets all new concepts. notice root is not included (not inserted in concept_dimension)
        Set<ConceptNode> newConcepts = conceptTree.newConceptNodes

        insertConceptsService.insert(newConcepts)

        RepeatStatus.FINISHED
    }

}
