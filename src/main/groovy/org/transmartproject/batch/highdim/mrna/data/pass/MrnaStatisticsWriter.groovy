package org.transmartproject.batch.highdim.mrna.data.pass

import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamSupport
import org.springframework.batch.item.ItemStreamWriter
import org.springframework.stereotype.Component
import org.transmartproject.batch.support.OnlineMeanAndVarianceCalculator

/**
 * For the first pass, just uses the data to compute a global mean
 * and variance.
 */
@Component
@StepScope
class MrnaStatisticsWriter extends ItemStreamSupport implements ItemStreamWriter<MrnaDataValue> {

    public final static String CALCULATOR_CTX_KEY = 'calculator'

    private final static double LOG_2 = Math.log(2d)

    // saved
    OnlineMeanAndVarianceCalculator meanAndVarianceCalculator

    @Override
    void write(List<? extends MrnaDataValue> items) throws Exception {
        items.each {
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
        executionContext.put(CALCULATOR_CTX_KEY, meanAndVarianceCalculator)
    }
}
