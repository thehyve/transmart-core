package org.transmartproject.batch.highdim.datastd

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamException
import org.springframework.batch.item.ItemStreamSupport
import org.springframework.batch.item.file.transform.FieldSet
import org.transmartproject.batch.batchartifacts.AbstractSplittingItemReader

/**
 * Calculates mean and variance of the log2 of a row's values.
 *
 */
@CompileStatic
@Slf4j
class PerDataRowLog2StatisticsListener extends ItemStreamSupport
        implements AbstractSplittingItemReader.EagerLineListener<DataPoint> {

    private final static double LOG_2 = Math.log(2)

    private final static String STATS_SUB_KEY = 'stats'

    private OnlineMeanAndVarianceCalculator stats
    private String statsKey

    PerDataRowLog2StatisticsListener() {
        name = 'perDataRow'
    }

    double getMean() {
        checkState()

        stats.mean / LOG_2
    }

    double getStandardDeviation() {
        checkState()

        stats.standardDeviation / LOG_2
    }

    private void checkState() {
        if (stats == null) {
            throw new IllegalStateException('No statistics calculated yet')
        }

        if (stats.n < 2) {
            throw new IllegalStateException('Did not get enough samples to ' +
                    'calculate the statistics; need two, got ' + stats.n)
        }
    }

    @Override
    void onLine(FieldSet fieldSet, Collection<DataPoint> items) {
        stats.reset()
        long ignoredCount = 0
        items.each {
            if (Double.isNaN(it.value) || it.value <= 0d) {
                ignoredCount++
                return
            }

            stats.push Math.log(it.value) // unscaled
        }

        log.debug("Annotation ${fieldSet.readString(0)}: mean=$mean, " +
                "stddev=$standardDeviation n=${stats.n} ignored=$ignoredCount")
    }

    @Override
    void open(ExecutionContext executionContext) throws ItemStreamException {
        statsKey = getExecutionContextKey(STATS_SUB_KEY)
        if (executionContext.containsKey(statsKey)) {
            stats = (executionContext.get(statsKey)
                    as OnlineMeanAndVarianceCalculator)
        } else {
            stats = new OnlineMeanAndVarianceCalculator()
        }
    }

    @Override
    void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.put(statsKey, stats.clone())
    }

    @Override
    @SuppressWarnings('CloseWithoutCloseable')
    void close() throws ItemStreamException {
        stats = null
    }
}
