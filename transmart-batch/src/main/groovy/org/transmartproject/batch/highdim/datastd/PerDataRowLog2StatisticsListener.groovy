package org.transmartproject.batch.highdim.datastd

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamException
import org.springframework.batch.item.ItemStreamSupport
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.beans.factory.annotation.Value
import org.transmartproject.batch.batchartifacts.AbstractSplittingItemReader

import static org.transmartproject.batch.stat.StatisticsCalculationUtils.log

/**
 * Calculates mean and variance of the log2 of a row's values.
 *
 */
@CompileStatic
@Slf4j
class PerDataRowLog2StatisticsListener extends ItemStreamSupport
        implements AbstractSplittingItemReader.EagerLineListener<DataPoint> {

    private final static double LOG_2 = Math.log(2)

    private final static String ROW_STATS_SUB_KEY = 'rowStatistics'

    private OnlineMeanAndVarianceCalculator rowStatistics
    private String statsKey

    @Value("#{jobExecutionContext['minPosDataSetValue']}")
    private Double minPosDataSetValue

    PerDataRowLog2StatisticsListener() {
        name = 'perDataRow'
    }

    double getMean() {
        checkState()

        rowStatistics.mean / LOG_2
    }

    double getStandardDeviation() {
        checkState()

        rowStatistics.standardDeviation / LOG_2
    }

    private void checkState() {
        if (rowStatistics == null) {
            throw new IllegalStateException('No statistics calculated yet')
        }
    }

    @Override
    void onLine(FieldSet fieldSet, Collection<DataPoint> keptItems) {
        rowStatistics.reset()
        keptItems.each {
            // division by log(2) to get binary logarithm is done inside getters: getMean() and getStandardDeviation()
            // it's done so to do not collect error and fro optimization reasons.
            rowStatistics.push log(it.value, minPosDataSetValue)
        }


        def annotationName = fieldSet.readString(0)
        log.debug("Annotation $annotationName: mean=$mean, " +
                "stddev=$standardDeviation n=${rowStatistics.n}")

        if (standardDeviation == 0.0d && rowStatistics.n > 0) {
            log.warn("Values for annotation $annotationName have zero " +
                    "standard deviation; their zscore will be NaN!")
        }
    }

    @Override
    void open(ExecutionContext executionContext) throws ItemStreamException {
        statsKey = getExecutionContextKey(ROW_STATS_SUB_KEY)
        if (executionContext.containsKey(statsKey)) {
            rowStatistics = (executionContext.get(statsKey)
                    as OnlineMeanAndVarianceCalculator)
        } else {
            rowStatistics = new OnlineMeanAndVarianceCalculator()
        }
    }

    @Override
    void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.put(statsKey, rowStatistics.clone())
    }

    @Override
    @SuppressWarnings('CloseWithoutCloseable')
    void close() throws ItemStreamException {
        rowStatistics = null
    }
}
