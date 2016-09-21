package org.transmartproject.batch.clinical.xtrial

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.batch.concept.ConceptFragment

/**
 * From the mappings read from the across trials file, fetch possibly xtrial
 * nodes from the database.
 */
class GatherXtrialNodesTasklet implements Tasklet {

    @Autowired
    XtrialMappingCollection mappingCollection

    @Autowired
    XtrialNodeRepository repository

    /* how many to pass to XtrialNodeRepository.getSubTree at once */
    int fetchSize = 20

    @Override
    RepeatStatus execute(StepContribution contribution,
                         ChunkContext chunkContext) throws Exception {
        mappingCollection.disjunctXtrialPrefixes
                .collate(fetchSize)
                .each { List<ConceptFragment> fragmentBlock ->
            List<XtrialNode> nodes = repository.getSubTree(*fragmentBlock)
            nodes.each {
                mappingCollection.registerTrialNode it
                contribution.incrementReadCount()
            }
        }

        RepeatStatus.FINISHED
    }
}
