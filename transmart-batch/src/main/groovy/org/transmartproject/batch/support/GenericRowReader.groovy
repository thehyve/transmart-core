package org.transmartproject.batch.support

import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamException
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.LineMapper
import org.springframework.batch.item.file.MultiResourceItemReader
import org.springframework.core.io.Resource

/**
 * Reader of Row that is prepared for multiple input Resources and implements LineMapper.<br>
 * It uses internally a FlatFileItemReader which skips the header.<br>
 * Should be subclassed by implementing addResources and mapLine
 */
abstract class GenericRowReader<T> extends MultiResourceItemReader<T> implements LineMapper<T> {

    void init() {
        setResources(resourcesToProcess as Resource[]) //sets the resources to read
        FlatFileItemReader<T> reader = new FlatFileItemReader<>()
        reader.setLineMapper(this)
        reader.setLinesToSkip(1) //ignores the header
        setDelegate(reader)
        setStrict(true) //we strictly must have data
    }

    abstract List<Resource> getResourcesToProcess()

    @Override
    void open(ExecutionContext executionContext) throws ItemStreamException {
        init()
        super.open(executionContext)
    }
}
