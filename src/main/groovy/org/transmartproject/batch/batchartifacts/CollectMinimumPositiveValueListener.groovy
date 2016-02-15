package org.transmartproject.batch.batchartifacts

import groovy.util.logging.Slf4j
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.AfterProcess
import org.springframework.batch.core.annotation.AfterStep
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStream
import org.springframework.batch.item.ItemStreamException
import org.springframework.batch.item.validator.ValidationException
import org.transmartproject.batch.highdim.datastd.DataPoint

/**
 * Collects minimal value of the data set and propagate it to the job execution context after step is finished.
 */
@Slf4j
class CollectMinimumPositiveValueListener implements ItemStream {

    public static final String MIN_POS_DATA_SET_VALUE = 'minPosDataSetValue'

    boolean minPositiveValueRequired = true

    private Double minPositiveValue

    @AfterProcess
    @SuppressWarnings('UnusedMethodParameter')
    void afterProcess(DataPoint input, DataPoint output) {
        if (output.value != null && !Double.isNaN(output.value) && output.value > 0) {
            //when minPositiveValue is null (initial value) assign it to the value
            if (minPositiveValue ==  null || minPositiveValue > output.value) {
                log.trace("Changing minimal positive value: ${minPositiveValue} => ${output.value}")
                minPositiveValue = output.value
            }
        }
    }

    @AfterStep
    void afterStep(StepExecution stepExecution) {
        if (stepExecution.exitStatus == ExitStatus.FAILED) {
            log.debug("Skipping promotion of minPositiveValue because the exit status is ${ExitStatus.FAILED}")
        } else {
            if (minPositiveValue == null) {
                if (minPositiveValueRequired) {
                    throw new ValidationException(
                            'No minimal positive value found. ' +
                                    'Are there any positive values (non zero) in the data set?')
                } else {
                    log.info("No minimal positive values has been found.")
                }
            } else {
                log.info("Minimal positive values has been found. It's ${minPositiveValue}.")
                stepExecution.jobExecution.executionContext.putDouble(MIN_POS_DATA_SET_VALUE, minPositiveValue)
            }
        }
    }

    @Override
    void open(ExecutionContext executionContext) throws ItemStreamException {
        if (executionContext.containsKey(MIN_POS_DATA_SET_VALUE)) {
            minPositiveValue = executionContext.getDouble(MIN_POS_DATA_SET_VALUE)
            log.info("Minimal positive value (${minPositiveValue}) has been restored from the execution context.")
        }
    }

    @Override
    void update(ExecutionContext executionContext) throws ItemStreamException {
        if (minPositiveValue) {
            log.debug("Put minimal positive value (${minPositiveValue}) into the context.")
            executionContext.putDouble(MIN_POS_DATA_SET_VALUE, minPositiveValue)
        }
    }

    @Override
    @SuppressWarnings('CloseWithoutCloseable')
    void close() throws ItemStreamException {}

}
