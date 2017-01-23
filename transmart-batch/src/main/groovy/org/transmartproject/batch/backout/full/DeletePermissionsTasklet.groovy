package org.transmartproject.batch.backout.full

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.batch.backout.BackoutComponent
import org.transmartproject.batch.secureobject.SecureObjectDAO
import org.transmartproject.batch.secureobject.SecureObjectToken

/**
 * Delete permission assignments to the study in question.
 */
@BackoutComponent
@Slf4j
class DeletePermissionsTasklet implements Tasklet {

    @Autowired
    SecureObjectToken secureObjectToken

    @Autowired
    SecureObjectDAO secureObjectDAO

    @Override
    RepeatStatus execute(StepContribution contribution,
                         ChunkContext chunkContext) throws Exception {
        assert secureObjectToken.toString() != 'EXP:PUBLIC'

        int affected = secureObjectDAO
                .deletePermissionsForSecureObject(secureObjectToken)

        log.debug "Deleted $affected permission assignments to $secureObjectToken"
        contribution.incrementWriteCount(affected)

        RepeatStatus.FINISHED
    }
}
