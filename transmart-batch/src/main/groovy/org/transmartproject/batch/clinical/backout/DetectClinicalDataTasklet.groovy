package org.transmartproject.batch.clinical.backout

import com.google.common.collect.Sets
import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.transmartproject.batch.backout.BackoutComponent
import org.transmartproject.batch.backout.BackoutContext
import org.transmartproject.batch.concept.ConceptNode
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.concept.ConceptTree
import org.transmartproject.batch.concept.ConceptType

/**
 * Tasklet to detect the existence of clinical data by looking at all the
 * concepts under the top node that are neither high dimensional, nor
 * parents to high dimensional.
 */
@BackoutComponent
@JobScope
@Slf4j
class DetectClinicalDataTasklet implements Tasklet {

    @Autowired
    ConceptTree conceptTree

    @Autowired
    BackoutContext backoutContext

    @Value("#{jobParameters['TOP_NODE']}")
    ConceptPath topNode

    @Override
    RepeatStatus execute(StepContribution contribution,
                         ChunkContext chunkContext) throws Exception {
        def irrelevantNodes = fetchIrrelevantNodes()

        Set<ConceptNode> clinicalRelatedNodes = Sets.difference(
                conceptTree.allConceptNodes,
                irrelevantNodes)

        log.debug("Found ${irrelevantNodes.size()} concepts between " +
                "high dimensional concepts and top node and their parents. " +
                "Full list: $irrelevantNodes")

        if (!clinicalRelatedNodes) {
            log.info('Found no clinical data related nodes')
        } else {
            log.info("Found ${clinicalRelatedNodes.size()} " +
                    "clinical data related nodes")
            log.debug("These are: $clinicalRelatedNodes")
            clinicalRelatedNodes.each { contribution.incrementReadCount() }
        }

        backoutContext.conceptsToDeleteBeforePromotion =
                (clinicalRelatedNodes*.path as Set)

        RepeatStatus.FINISHED
    }

    private Set<ConceptNode> fetchIrrelevantNodes() {
        Set<ConceptNode> seenConceptNodes = [] as Set

        conceptTree.allConceptNodes.each { ConceptNode node ->
            if (node.type == ConceptType.HIGH_DIMENSIONAL ||
                    node.path == topNode) {
                def curNode = node
                while (curNode != null) {
                    if (curNode in seenConceptNodes) {
                        break
                    }
                    seenConceptNodes << curNode
                    curNode = conceptTree.parentFor(curNode)
                }
            }
        }

        seenConceptNodes
    }
}
