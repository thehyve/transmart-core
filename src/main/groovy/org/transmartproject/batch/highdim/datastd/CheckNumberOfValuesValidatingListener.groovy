package org.transmartproject.batch.highdim.datastd

import org.springframework.batch.item.ItemStreamSupport
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.batch.item.validator.ValidationException
import org.transmartproject.batch.batchartifacts.AbstractSplittingItemReader

/**
 * Checks that the line for a probe has enough values for the variance to be
 * calculated.
 */
class CheckNumberOfValuesValidatingListener extends ItemStreamSupport implements
        AbstractSplittingItemReader.EagerLineListener<DataPoint> {

    @Override
    void onLine(FieldSet fieldSet, Collection<DataPoint> items) {
        int c = 0
        for (DataPoint p in items) {
            if (p.value > 0) {
                c++
            }

            if (c >= 2) {
                return
            }
        }

        String annotation = fieldSet.readString(0)
        throw new ValidationException("The annotation $annotation does not " +
                "have enough positive values for the statistics to be " +
                "calculated (needs 2, got $c)")
    }
}
