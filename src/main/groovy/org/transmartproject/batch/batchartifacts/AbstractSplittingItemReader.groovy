package org.transmartproject.batch.batchartifacts

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.item.*
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

    protected FieldSet currentFieldSet // saved
    protected int position // saved
    protected int upstreamPos
    protected ItemStreamReader<FieldSet> delegate

    private final static String SAVED_FIELD_SET_KEY = 'savedFieldSet'
    private final static String SAVED_POSITION = 'position'
    private final static String SAVED_UPSTREAM_POS = 'upstreamPos'

    @Override
    @SuppressWarnings('CatchException')
    T read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (currentFieldSet == null || position >= currentFieldSet.fieldCount) {
            try {
                currentFieldSet = fetchNextDelegateLine()
                position = 0
            } catch (Exception e) {
                log.error "Exception fetching ${upstreamPos + 1}-th line from delegate", e
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
            log.error "Exception processing ${upstreamPos}-th line gotten " +
                    "from delegate, column $position, fieldset " +
                    "$currentFieldSet", e
            throw e
        } finally {
            position++
        }
    }

    // return null to signal end
    protected FieldSet fetchNextDelegateLine() {
        def result = delegate.read()
        upstreamPos++
        result
    }

    abstract protected T doRead()

    @Override
    void open(ExecutionContext executionContext) throws ItemStreamException {
        if (executionContext.containsKey(
                getExecutionContextKey(SAVED_POSITION))) {
            position = executionContext.getInt(
                    getExecutionContextKey(SAVED_POSITION))
            upstreamPos = executionContext.getInt(
                    getExecutionContextKey(SAVED_UPSTREAM_POS))
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
        executionContext.putInt(
                getExecutionContextKey(SAVED_UPSTREAM_POS),
                upstreamPos)
    }

    @Override
    @SuppressWarnings('CloseWithoutCloseable')
    void close() throws ItemStreamException {
        log.trace("Closing $this")
        upstreamPos = 0
        position = 0
        currentFieldSet = null
    }
}
