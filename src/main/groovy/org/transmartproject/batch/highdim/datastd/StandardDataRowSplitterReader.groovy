package org.transmartproject.batch.highdim.datastd

import groovy.transform.CompileStatic
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.beans.factory.annotation.Value
import org.transmartproject.batch.batchartifacts.AbstractSplittingItemReader

/**
 * Splits a standard data file row into several {@link StandardDataValue}s.
 * Needs to have the delegate set manually (no autowiring).
 *
 * Should be step-scoped.
 */
@CompileStatic
class StandardDataRowSplitterReader extends AbstractSplittingItemReader<StandardDataValue> {

    @Value('#{stepExecution.executionContext}')
    private ExecutionContext stepExecutionContext

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private List<String> columnNames = (List<String>) ({
        stepExecutionContext.get('tsv.header')
    })()

    private String lastAnnotationName

    // to be configured
    // to avoid instantiating a new object in the processor to convert
    // from standard to triple data
    Class<? extends StandardDataValue> dataPointClass = StandardDataValue

    @Override
    protected StandardDataValue doRead() {
        if (position == 0) {
            // skip position 0
            position++
            lastAnnotationName = null
            return doRead()
        }

        if (lastAnnotationName == null) {
            lastAnnotationName = currentFieldSet.readString(0)
        }

        // has to be provided
        Double value = currentFieldSet.readDouble(position)

        dataPointClass.newInstance(
                sampleCode: columnNames[position],
                annotation: lastAnnotationName,
                value: value)
    }

    void setDelegate(ItemStreamReader<FieldSet> innerReader) {
        super.delegate = innerReader
    }
}
