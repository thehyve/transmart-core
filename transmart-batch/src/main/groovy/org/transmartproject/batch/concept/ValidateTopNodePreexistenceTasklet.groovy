package org.transmartproject.batch.concept

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced

/**
 * Throws an {@link IllegalStateException} if the top node doesn't exist already
 * or has the wrong type.
 */
@Component
@JobScopeInterfaced
class ValidateTopNodePreexistenceTasklet implements Tasklet {

    @Value("#{jobParameters['TOP_NODE']}")
    ConceptPath topNode

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Autowired
    ConceptTree conceptTree

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        ConceptNode node = conceptTree[topNode]
        if (!node || !conceptTree.isSavedNode(node)) {
            throw new IllegalStateException(
                    "The specified top node $topNode either doesn't exist already " +
                            "or doesn't belong to the study $studyId")
        }

        if (node.type != ConceptType.CATEGORICAL) {
            throw new IllegalStateException("Unexpected node type for $node")
        }

        RepeatStatus.FINISHED
    }
}
