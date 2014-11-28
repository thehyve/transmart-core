package org.transmartproject.batch.batchartifacts

import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener

/**
 * If notifyOnFound is false and no items are read, changes the exit status to
 * 'NOT FOUND'.
 * If notifyOnFound is true and items are read, changes the exit status to
 * 'FOUND'
 */
class FoundExitStatusChangeListener implements StepExecutionListener {

    boolean notifyOnFound = true

    @Override
    void beforeStep(StepExecution stepExecution) {}

    @Override
    ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.exitStatus == ExitStatus.COMPLETED &&
                stepExecution.readCount > 0 && notifyOnFound) {
            new ExitStatus('FOUND')
        } else if (stepExecution.exitStatus == ExitStatus.COMPLETED &&
                stepExecution.readCount == 0 && !notifyOnFound) {
            new ExitStatus('NOT FOUND')
        } else {
            stepExecution.exitStatus
        }
    }
}
