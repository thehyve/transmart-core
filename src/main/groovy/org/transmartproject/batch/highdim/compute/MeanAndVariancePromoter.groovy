package org.transmartproject.batch.highdim.compute

import groovy.util.logging.Slf4j
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ExecutionContext
import org.springframework.stereotype.Component

/**
 * Writes the mean and variance calculations into the context.
 */
@Component
@StepScope
@Slf4j
class MeanAndVariancePromoter implements StepExecutionListener {

    public final static String CALCULATED_MEAN = 'calculatedMean'
    public final static String CALCULATED_VARIANCE = 'calculatedVariance'
    public final static String CALCULATED_N = 'calculatedN'

    @Override
    void beforeStep(StepExecution stepExecution) {}

    @Override
    ExitStatus afterStep(StepExecution stepExecution) {
        ExecutionContext stepExecutionContext = stepExecution.executionContext
        ExecutionContext jobExecutionContext = stepExecution.jobExecution.executionContext
        OnlineMeanAndVarianceCalculator calculator = stepExecutionContext.get(
                DataPointLogStatisticsWriter.CALCULATOR_CTX_KEY)
        log.info("Saving calculated statistics: $calculator")
        jobExecutionContext.putDouble(CALCULATED_MEAN, calculator.mean)
        jobExecutionContext.putDouble(CALCULATED_VARIANCE, calculator.variance)
        jobExecutionContext.putDouble(CALCULATED_N, calculator.n)
    }
}
