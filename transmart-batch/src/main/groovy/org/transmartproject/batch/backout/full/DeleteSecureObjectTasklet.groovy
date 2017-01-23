package org.transmartproject.batch.backout.full

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.batch.backout.BackoutComponent
import org.transmartproject.batch.secureobject.SecureObjectDAO
import org.transmartproject.batch.secureobject.SecureObjectToken

/**
 * Deletes a "secure object" from bio_experiment and search_secure_object.
 */
@BackoutComponent
class DeleteSecureObjectTasklet implements Tasklet {

    @Autowired
    SecureObjectToken token

    @Autowired
    SecureObjectDAO secureObjectDAO

    @Override
    RepeatStatus execute(StepContribution contribution,
                         ChunkContext chunkContext) throws Exception {

        int affected = secureObjectDAO.deleteSecureObject(token)
        contribution.incrementWriteCount(affected)

        RepeatStatus.FINISHED
    }
}
