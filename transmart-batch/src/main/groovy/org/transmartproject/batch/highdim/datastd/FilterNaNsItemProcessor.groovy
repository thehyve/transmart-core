package org.transmartproject.batch.highdim.datastd

import org.springframework.batch.item.ItemProcessor
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Filters out {@link DataPoint} elements whose value is NaN.
 */
@Component
@Scope('singleton')
class FilterNaNsItemProcessor implements ItemProcessor<DataPoint, DataPoint> {
    @Override
    DataPoint process(DataPoint item) throws Exception {
        if (item.value != null && !Double.isNaN(item.value)) {
            item
        } // else null (filter out)
    }
}
