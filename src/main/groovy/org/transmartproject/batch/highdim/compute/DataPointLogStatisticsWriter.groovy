package org.transmartproject.batch.highdim.compute

import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamSupport
import org.springframework.batch.item.ItemStreamWriter
import org.springframework.stereotype.Component

/**
 * For the first pass. Calculates the mean and variance of the log2 of the
 * data point.
 */
@Component
@StepScope
@Slf4j
class DataPointLogStatisticsWriter extends ItemStreamSupport
        implements ItemStreamWriter<DataPoint> {

    public final static String CALCULATOR_CTX_KEY = 'calculator'

    private final static double LOG_2 = Math.log(2d)

    // saved
    OnlineMeanAndVarianceCalculator meanAndVarianceCalculator

    @Override
    void write(List<? extends DataPoint> items) throws Exception {
        items.each {
            if (it.value <= 0d) {
                log.info("Ignored value $it for mean/variance calculation")
                return
            }
            meanAndVarianceCalculator.push Math.log(it.value) / LOG_2
        }
    }

    @Override
    void open(ExecutionContext executionContext) {
        meanAndVarianceCalculator = executionContext.get(CALCULATOR_CTX_KEY) ?:
                new OnlineMeanAndVarianceCalculator()
    }

    @Override
    void update(ExecutionContext executionContext) {
        executionContext.put(CALCULATOR_CTX_KEY,
                meanAndVarianceCalculator.clone())
    }
}
