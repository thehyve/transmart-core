package org.transmartproject.batch.highdim.datastd

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener
import org.springframework.batch.item.ItemProcessor

/**
 * Prints warnings when finding non-NaN, negative numbers.
 */
@CompileStatic
@Slf4j
class NegativeDataPointWarningProcessor
        implements ItemProcessor<StandardDataValue, StandardDataValue>, StepExecutionListener {

    private final static int MAX_VIOLATORS_STORED = 10

    private final Queue<StandardDataValue> violators = [] as Queue

    private long violatorsCount = 0

    @Override
    void beforeStep(StepExecution stepExecution) {}

    @Override
    ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.exitStatus.exitCode != 'COMPLETED') {
            return
        }
        if (violatorsCount <= 1) {
            return
        }

        if (violatorsCount <= violators.size()) {
            log.warn("Found $violatorsCount negative non-NaN values: " +
                    "$violators")
        } else {
            log.warn("Found $violatorsCount negative non-NaN values. The " +
                    "first $MAX_VIOLATORS_STORED were: $violators")
        }

        stepExecution.exitStatus
    }

    @Override
    StandardDataValue process(StandardDataValue item) throws Exception {
        if (item.value == null || item.value >= 0 || Double.isNaN(item.value)) {
            return item
        }

        violatorsCount++
        if (violatorsCount == 1) {
            log.warn("Found negative non-NaN value: $item. " +
                    "Further occurrences will be collected, " +
                    "until a maximum of $MAX_VIOLATORS_STORED")
        }

        if (violatorsCount <= MAX_VIOLATORS_STORED) {
            violators << item
        }

        item
    }
}
