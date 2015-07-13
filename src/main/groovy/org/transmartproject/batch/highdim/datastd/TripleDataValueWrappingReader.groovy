package org.transmartproject.batch.highdim.datastd

import org.springframework.batch.item.ItemReader
import org.springframework.beans.factory.annotation.Autowired

/**
 * Calculates log intensity and zscore; converts non-positives to NaNs.
 *
 * No state, so can be singleton.
 *
 * Cannot be a processor because spring batch reads a bunch of items and then
 * passes them one by one to the processor instead of reading one item and
 * passing it immediately to the processor before reading another. By that time,
 * the statistics have probably changed already.
 */
class TripleDataValueWrappingReader implements ItemReader<TripleStandardDataValue> {

    private static final double LOG_2 = Math.log(2)

    ItemReader<TripleStandardDataValue> delegate

    @Autowired
    private PerDataRowLog2StatisticsListener statisticsListener


    private double clamp(double lowerBound, double upperBound, double value) {
        Math.min(upperBound, Math.max(lowerBound, value))
    }

    @Override
    TripleStandardDataValue read() throws Exception {
        TripleStandardDataValue item = delegate.read()
        if (item != null) {
            process item
        }
    }

    private TripleStandardDataValue process(TripleStandardDataValue item) throws Exception {
        if (item.value <= 0 || Double.isNaN(item.value)) {
            item.value = item.logValue = item.zscore = Double.NaN
            return item
        }

        item.logValue = Math.log(item.value) / LOG_2
        item.zscore = clamp(-2.5d, 2.5d,
                (item.logValue - statisticsListener.mean) /
                        statisticsListener.standardDeviation)

        item
    }
}
