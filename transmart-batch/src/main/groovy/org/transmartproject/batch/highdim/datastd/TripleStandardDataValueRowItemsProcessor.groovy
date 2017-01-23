package org.transmartproject.batch.highdim.datastd

import org.springframework.batch.core.StepExecution
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Value
import org.transmartproject.batch.batchartifacts.HeaderParsingLineCallbackHandler

import static org.transmartproject.batch.stat.StatisticsCalculationUtils.clamp

/**
 * Process row of the standard data values
 */
class TripleStandardDataValueRowItemsProcessor implements RowItemsProcessor<TripleStandardDataValue> {

    OnlineMeanAndVarianceCalculator rowStatistics = new OnlineMeanAndVarianceCalculator()

    ItemProcessor<TripleStandardDataValue, TripleStandardDataValue> itemsPreProcessor

    Boolean zscoreProvided = null

    boolean isZscoreProvided() {
        if (zscoreProvided == null) {
            zscoreProvided = stepExecution.executionContext
                    .get(HeaderParsingLineCallbackHandler.PARSED_HEADER_OUT_KEY)
                    .any { key, value -> 'zscore' == value.suffix }
        }
        zscoreProvided
    }

    @Value('#{stepExecution}')
    StepExecution stepExecution

    List<TripleStandardDataValue> process(List<TripleStandardDataValue> rowItems) {


        List<TripleStandardDataValue> resultRowItems = []
        rowItems.each {
            TripleStandardDataValue tsdVal = itemsPreProcessor.process(it)
            if (tsdVal) {
                resultRowItems.add(tsdVal)

            }
        }

        if (!isZscoreProvided()) {
            updateWithCalculatedZscore(resultRowItems)
        }

        resultRowItems
    }

    private void updateWithCalculatedZscore(List<TripleStandardDataValue> resultRowItems) {
        rowStatistics.reset()
        resultRowItems.each {
            rowStatistics.push it.logValue
        }

        double stdDiv = rowStatistics.standardDeviation
        double mean = rowStatistics.mean
        resultRowItems.each {
            if (stdDiv > 0) {
                it.zscore = clamp(-2.5d, 2.5d,
                        (it.logValue - mean) / stdDiv)
            }
        }
    }

}
