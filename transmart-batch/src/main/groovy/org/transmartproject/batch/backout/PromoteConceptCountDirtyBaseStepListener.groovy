package org.transmartproject.batch.backout

import groovy.util.logging.Slf4j
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.listener.StepExecutionListenerSupport
import org.springframework.beans.factory.annotation.Autowired

/**
 * Simply calls {@link BackoutContext#promoteFactCountDirtiness()} as long
 * as the exit code is not FAILED.
 */
@BackoutComponent
@Slf4j
class PromoteConceptCountDirtyBaseStepListener extends StepExecutionListenerSupport {

    @Autowired
    BackoutContext backountContext

    @Override
    ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.exitStatus == ExitStatus.FAILED) {
            log.debug('Skipping promotion of concept count dirt base because ' +
                    "the exit status is ${ExitStatus.FAILED}")
        } else {
            backountContext.promoteFactCountDirtiness()
        }
    }
}
