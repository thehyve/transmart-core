package org.transmartproject.batch.batchartifacts

import groovy.util.logging.Slf4j
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener

/**
 * Logs the number of items read, written and skipped after each step.
 */
@Slf4j
class LogCountsStepListener implements StepExecutionListener {
    @Override
    void beforeStep(StepExecution stepExecution) {}

    @Override
    ExitStatus afterStep(StepExecution s) {
        log.info("READ: ${s.readCount}, " +
                "WRITTEN: ${s.writeCount}, " +
                "SKIPPED: ${s.skipCount}")
    }
}
