package org.transmartproject.batch.highdim.datastd

import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Value

import static org.transmartproject.batch.stat.StatisticsCalculationUtils.log

/**
 * Calculates log field of @link TripleStandardDataValue bean.
 */
class TripleStandardDataValueLogCalculationProcessor
        implements ItemProcessor<TripleStandardDataValue, TripleStandardDataValue> {

    private static final double LOG_2 = Math.log(2)

    @Value("#{jobExecutionContext['minPosDataSetValue']}")
    private Double minPosDataSetValue

    @Override
    TripleStandardDataValue process(TripleStandardDataValue item) throws Exception {
        if (item.value != null) {
            item.logValue = log(item.value, minPosDataSetValue) / LOG_2
        }

        item
    }

}
