package org.transmartproject.batch.highdim.metabolomics.platform

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced

/**
 * Simply calls {@link MetabolomicsBiochemicalsPile#assignIds()}.
 */
@Component
@JobScopeInterfaced
@Slf4j
class MetabolomicsAssignIdsTasklet implements Tasklet {
    @Autowired
    MetabolomicsBiochemicalsPile pile

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        long numberOfIds = pile.assignIds()
        assert numberOfIds <= Integer.MAX_VALUE
        contribution.incrementWriteCount(numberOfIds as int)
    }
}
