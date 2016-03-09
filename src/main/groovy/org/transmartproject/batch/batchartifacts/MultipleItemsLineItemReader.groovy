package org.transmartproject.batch.batchartifacts

import com.google.common.collect.Lists
import org.springframework.batch.item.*
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.LineCallbackHandler
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream
import org.springframework.batch.item.file.mapping.DefaultLineMapper
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper
import org.springframework.batch.item.file.separator.DefaultRecordSeparatorPolicy
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.batch.item.validator.ValidationException
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.io.Resource
import org.springframework.util.Assert
import org.springframework.util.ClassUtils

import static org.springframework.batch.item.file.transform.DelimitedLineTokenizer.DELIMITER_TAB

/**
 * Reads wide file format where each row contains normally more than one item.
 */
class MultipleItemsLineItemReader<T> extends ItemStreamSupport
        implements ResourceAwareItemReaderItemStream<T>, InitializingBean, Closeable {

    {
        setName(ClassUtils.getShortName(getClass()))
    }

    private static final String SAVED_CACHE = 'savedCache'
    Resource resource
    MultipleItemsFieldSetMapper<T> multipleItemsFieldSetMapper

    private FlatFileItemReader<FieldSet> flatFileItemReader
    private Collection<T> cache

    @Override
    T read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!cache) {
            FieldSet fieldSet = flatFileItemReader.read()
            if (fieldSet != null) {
                cache = multipleItemsFieldSetMapper.mapFieldSet(fieldSet)
            }
        }
        if (cache) {
            T item = cache[0]
            cache.remove(item)
            item
        }
    }

    @Override
    void afterPropertiesSet() throws Exception {
        Assert.notNull(multipleItemsFieldSetMapper, 'mapper has to be specified')

        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer(
                delimiter: DELIMITER_TAB
        )
        flatFileItemReader = new FlatFileItemReader(
                lineMapper: new DefaultLineMapper(
                        lineTokenizer: lineTokenizer,
                        fieldSetMapper: new PassThroughFieldSetMapper(),
                ),
                recordSeparatorPolicy: new DefaultRecordSeparatorPolicy(),
                resource: resource,
                linesToSkip: 1,
                skippedLinesCallback: new LineCallbackHandler() {
                    @Override
                    void handleLine(String line) {
                        String[] header = new DelimitedLineTokenizer(DELIMITER_TAB)
                                .tokenize(line).values
                        Map uniqueHeaderNames = header.groupBy()
                        if (header.length > uniqueHeaderNames.size()) {
                            Set<String> violators = uniqueHeaderNames.findAll { it.value.size() > 1 }.keySet()
                            throw new ValidationException("${violators} appear(s) more than once in the header.")
                        }
                        lineTokenizer.names = header
                    }
                },
        )
    }

    @Override
    void open(ExecutionContext executionContext) throws ItemStreamException {
        cache = (Collection) executionContext.get(
                getExecutionContextKey(SAVED_CACHE))
        flatFileItemReader.open(executionContext)
    }

    @Override
    void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.put(
                getExecutionContextKey(SAVED_CACHE),
                cache ? Lists.newLinkedList(cache) : null)
        flatFileItemReader.update(executionContext)
    }

    @Override
    void close() throws ItemStreamException {
        flatFileItemReader.close()
    }
}
