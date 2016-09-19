package org.transmartproject.batch.batchartifacts

import groovy.util.logging.Slf4j
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener

/**
 * If notifyOnFound is false and no items are read, changes the exit status to
 * 'NOT FOUND'.
 * If notifyOnFound is true and items are read, changes the exit status to
 * 'FOUND'
 */
@Slf4j
class FoundExitStatusChangeListener implements StepExecutionListener {

    boolean notifyOnFound = true

    @Override
    void beforeStep(StepExecution stepExecution) {}

    @Override
    ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.exitStatus == ExitStatus.COMPLETED &&
                stepExecution.readCount > 0 && notifyOnFound) {
            log.debug("Changing exit code to 'FOUND'")
            new ExitStatus('FOUND')
        } else if (stepExecution.exitStatus == ExitStatus.COMPLETED &&
                stepExecution.readCount == 0 && !notifyOnFound) {
            log.debug("Changing exit code to 'NOT FOUND'")
            new ExitStatus('NOT FOUND')
        } else {
            log.debug("Keeping exit code ${stepExecution.exitStatus.exitCode}")
            stepExecution.exitStatus
        }
    }
}
