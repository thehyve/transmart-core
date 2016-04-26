package org.transmartproject.batch.highdim.datastd

import org.springframework.batch.item.ItemReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

import static org.transmartproject.batch.stat.StatisticsCalculationUtils.clamp
import static org.transmartproject.batch.stat.StatisticsCalculationUtils.log

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
    private PerDataRowLog2StatisticsListener perRowStatisticsListener

    @Value("#{jobExecutionContext['minPosDataSetValue']}")
    private Double minPosDataSetValue

    @Override
    TripleStandardDataValue read() throws Exception {
        TripleStandardDataValue item = delegate.read()
        if (item != null) {
            process item
        }
    }

    private TripleStandardDataValue process(TripleStandardDataValue item) throws Exception {
        item.logValue = log(item.value, minPosDataSetValue) / LOG_2

        if (perRowStatisticsListener) {
            double stdDiv = perRowStatisticsListener.standardDeviation
            if (stdDiv > 0) {
                item.zscore = clamp(-2.5d, 2.5d,
                        (item.logValue - perRowStatisticsListener.mean) / stdDiv)
            }
        }

        item
    }
}
