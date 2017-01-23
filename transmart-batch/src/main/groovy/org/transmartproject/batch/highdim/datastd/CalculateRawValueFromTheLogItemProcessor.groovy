package org.transmartproject.batch.highdim.datastd

import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Value

/**
 * Calculates raw value field of @link TripleStandardDataValue bean from the log value.
 */
class CalculateRawValueFromTheLogItemProcessor
        implements ItemProcessor<TripleStandardDataValue, TripleStandardDataValue> {

    private double srcLogBase

    @Override
    TripleStandardDataValue process(TripleStandardDataValue item) throws Exception {
        if (item.value != null) {
            item.value = Math.pow(srcLogBase, item.value)
        }

        item
    }

    @Value("#{jobParameters['SRC_LOG_BASE']}")
    void setSrcLogBase(String srcLogBaseString) {
        this.srcLogBase = Double.parseDouble(srcLogBaseString)
    }
}
