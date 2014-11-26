package org.transmartproject.batch.batchartifacts

import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener

/**
 * If no items are read, changes the exit status to 'NOT FOUND'.
 */
class FoundExitStatusChangeListener implements StepExecutionListener {
    @Override
    void beforeStep(StepExecution stepExecution) {}

    @Override
    ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.exitStatus == ExitStatus.COMPLETED &&
                stepExecution.readCount > 0) {
            new ExitStatus('FOUND')
        } else {
            stepExecution.exitStatus
        }
    }
}
