package org.transmartproject.batch.batchartifacts

import com.google.common.collect.Lists
import groovy.util.logging.Slf4j
import org.springframework.batch.item.*
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.LineCallbackHandler
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream
import org.springframework.batch.item.file.mapping.DefaultLineMapper
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper
import org.springframework.batch.item.file.separator.DefaultRecordSeparatorPolicy
import org.springframework.batch.item.file.transform.DefaultFieldSetFactory
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.batch.item.file.transform.FieldSetFactory
import org.springframework.batch.item.validator.ValidationException
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.io.Resource
import org.springframework.util.Assert
import org.springframework.util.ClassUtils
import org.transmartproject.batch.support.ScientificNotationFormat

import static org.springframework.batch.item.file.transform.DelimitedLineTokenizer.DELIMITER_TAB

/**
 * Reads wide file format where each row contains normally more than one item.
 */
@Slf4j
class MultipleItemsLineItemReader<T> extends ItemStreamSupport
        implements ResourceAwareItemReaderItemStream<T>, InitializingBean, Closeable {

    {
        setName(ClassUtils.getShortName(getClass()))
    }

    /**
     * Resource to read data from (required)
     */
    Resource resource
    /**
     * Maps a field set to the multiple items (required)
     */
    MultipleItemsFieldSetMapper<T> multipleItemsFieldSetMapper

    private static final String SAVED_ITEM_POS_IN_CACHE = 'savedItemPositionInTheCache'

    private final List<T> cache = Lists.<T> newLinkedList()
    private int savedItemPositionInTheCache = 0
    private FlatFileItemReader<FieldSet> flatFileItemReader

    @Override
    T read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (cache.size() <= savedItemPositionInTheCache) {
            FieldSet fieldSet = flatFileItemReader.read()
            if (fieldSet != null) {
                savedItemPositionInTheCache = 0
                cache.clear()
                Collection<T> items = multipleItemsFieldSetMapper.mapFieldSet(fieldSet)
                cache.addAll(items)
            }
        }
        if (cache.size() > savedItemPositionInTheCache) {
            cache.get(savedItemPositionInTheCache++)
        }
    }

    @Override
    void afterPropertiesSet() throws Exception {
        Assert.notNull(resource, 'resource has to be specified')
        Assert.notNull(multipleItemsFieldSetMapper, 'mapper has to be specified')

        FieldSetFactory fieldSetFactory = new DefaultFieldSetFactory(
                numberFormat: new ScientificNotationFormat()
        )

        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer(
                delimiter: DELIMITER_TAB,
                fieldSetFactory: fieldSetFactory
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
                        String[] header = lineTokenizer.tokenize(line).values
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
        if (executionContext.containsKey(
                getExecutionContextKey(SAVED_ITEM_POS_IN_CACHE))) {
            savedItemPositionInTheCache = executionContext.getInt(
                    getExecutionContextKey(SAVED_ITEM_POS_IN_CACHE))
            log.debug("Load the item position (${savedItemPositionInTheCache}) from the context.")
        }
        flatFileItemReader.open(executionContext)
    }

    @Override
    void update(ExecutionContext executionContext) throws ItemStreamException {
        log.debug("Save the item position (${savedItemPositionInTheCache}) to the context.")
        executionContext.put(
                getExecutionContextKey(SAVED_ITEM_POS_IN_CACHE), savedItemPositionInTheCache)
        flatFileItemReader.update(executionContext)
    }

    @Override
    void close() throws ItemStreamException {
        cache.clear()
        savedItemPositionInTheCache = 0
        flatFileItemReader.close()
    }
}
