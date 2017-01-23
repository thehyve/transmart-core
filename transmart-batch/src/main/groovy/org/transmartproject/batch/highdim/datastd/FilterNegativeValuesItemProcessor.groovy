package org.transmartproject.batch.highdim.datastd

import org.springframework.batch.item.ItemProcessor
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Filters out {@link DataPoint} elements whose value is negative.
 */
@Component
@Scope('singleton')
class FilterNegativeValuesItemProcessor implements ItemProcessor<DataPoint, DataPoint> {
    @Override
    DataPoint process(DataPoint item) throws Exception {
        if (item.value >= 0) {
            item
        } // else null (filter out)
    }
}
