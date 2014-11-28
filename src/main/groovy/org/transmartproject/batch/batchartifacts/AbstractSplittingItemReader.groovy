package org.transmartproject.batch.batchartifacts

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Slf4j
import org.springframework.batch.item.*
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.util.ClassUtils

/**
 * Takes input from another {@link ItemStreamReader} and breaks its items into
 * several items.
 *
 * Do not forget to register the delegate stream! See
 * {@url http://docs.spring.io/spring-batch/reference/html/configureStep.html#registeringItemStreams}.
 */
@Slf4j
@CompileStatic
abstract class AbstractSplittingItemReader<T> extends ItemStreamSupport implements ItemStreamReader<T> {

    {
        setName(ClassUtils.getShortName(getClass()))
    }

    protected FieldSet currentFieldSet
    protected int position
    protected ItemStreamReader<FieldSet> delegate

    private final static String SAVED_FIELD_SET_KEY = 'savedFieldSet'
    private final static String SAVED_POSITION = 'position'

    @Override
    @SuppressWarnings('CatchException')
    T read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (currentFieldSet == null || position >= currentFieldSet.fieldCount) {
            try {
                currentFieldSet = fetchNextDelegateLine()
                position = 0
            } catch (Exception e) {
                log.error "Exception fetching line ${line} from delegate", e
                throw e
            }
        }
        if (currentFieldSet == null) {
            log.debug("Delegate $delegate returned null to $this")
            return null
        }

        try {
            doRead()
        } catch (Exception e) {
            log.error "Exception processing line ${line}, column $position, " +
                    "fieldset $currentFieldSet", e
            throw e
        } finally {
            position++
        }
    }

    // return null to signal end
    protected FieldSet fetchNextDelegateLine() {
        delegate.read()
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    int getLine() {
        delegate instanceof FlatFileItemReader ? delegate.lineCount : -1
    }

    abstract protected T doRead()

    @Override
    void open(ExecutionContext executionContext) throws ItemStreamException {
        if (executionContext.containsKey(
                getExecutionContextKey(SAVED_POSITION))) {
            position = executionContext.getInt(
                    getExecutionContextKey(SAVED_POSITION))
        }
        currentFieldSet = (FieldSet) executionContext.get(
                getExecutionContextKey(SAVED_FIELD_SET_KEY))

        log.debug "Opened $this, got position $position " +
                "on fieldset $currentFieldSet"
    }

    @Override
    void update(ExecutionContext executionContext) throws ItemStreamException {
        log.debug "Saving state with position $position on fieldset; $currentFieldSet"
        executionContext.put(
                getExecutionContextKey(SAVED_FIELD_SET_KEY),
                currentFieldSet)
        executionContext.putInt(
                getExecutionContextKey(SAVED_POSITION),
                position)
    }

    @Override
    @SuppressWarnings('CloseWithoutCloseable')
    void close() throws ItemStreamException {
        log.trace("Closing $this")
    }
}
