package org.transmartproject.batch.batchartifacts

import com.google.common.collect.Lists
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.item.*
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.util.ClassUtils

/**
 * Takes input from another {@link ItemStreamReader} and breaks its items into
 * several items.
 *
 */
@Slf4j
@CompileStatic
abstract class AbstractSplittingItemReader<T> extends ItemStreamSupport implements ItemStreamReader<T> {

    {
        setName(ClassUtils.getShortName(getClass()))
    }

    protected FieldSet currentFieldSet // saved
    protected int position             // saved
    protected int upstreamPos          // saved
    private Queue<T> cachedValues      // saved, only if eagerLineProcessor

    // to be configured
    protected ItemStreamReader<FieldSet> delegate // should not be registered as stream
    EagerLineListener<T> eagerLineListener
    /**
     * Filtering happens before the line listener {@link this.eagerLineListener} is called.
     */
    ItemProcessor<T, T> earlyItemProcessor

    private final static String SAVED_FIELD_SET_KEY = 'savedFieldSet'
    private final static String SAVED_POSITION = 'position'
    private final static String SAVED_UPSTREAM_POS = 'upstreamPos'
    private final static String SAVED_CACHED_ITEMS = 'cachedItems'

    @Override
    T read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (eagerLineListener) {
            cachedRead()
        } else {
            uncachedRead()
        }
    }

    @SuppressWarnings('CatchException')
    private T cachedRead() {
        if (!cachedValues) { // empty or null
            assert needsDelegateFetch()
            wrappedDelegateLineFetch()

            cachedValues = [] as Queue
            boolean sawData = false
            while (!needsDelegateFetch()) {
                def value = uncachedRead()
                if (value != null) {
                    sawData = true
                    if (earlyItemProcessor) {
                        value = earlyItemProcessor.process(value)
                    }
                    if (value != null) {
                        cachedValues << value
                    }
                } else {
                    // we'll break out next:
                    assert needsDelegateFetch()
                }
            }

            if (!cachedValues && !sawData) {
                return null
            }

            try {
                eagerLineListener.onLine(currentFieldSet, cachedValues)
            } catch (Exception e) {
                log.error "Exception calling eager line listener on " +
                        "${upstreamPos}-th line gotten from delegate, " +
                        "fieldset '$currentFieldSet', items '$cachedValues'", e
                throw e
            }

            if (!cachedValues /* && sawData */) {
                // we filtered out the whole line
                return cachedRead()
            }
        }

        cachedValues.remove()
    }

    @SuppressWarnings('CatchException')
    private T uncachedRead() {
        if (needsDelegateFetch()) {
            wrappedDelegateLineFetch()
        }

        if (currentFieldSet == null) {
            // signals the end of the delegate
            log.debug("Delegate $delegate returned null to $this")
            return null
        }

        try {
            T result = doRead()
            position++
            result
        } catch (Exception e) {
            log.error "Exception processing ${upstreamPos}-th line gotten " +
                    "from delegate, column $position, fieldset " +
                    "$currentFieldSet", e
            throw e
        }
    }

    @SuppressWarnings('CatchException')
    private void wrappedDelegateLineFetch() {
        try {
            upstreamPos++
            currentFieldSet = fetchNextDelegateLine()
            position = 0
        } catch (Exception e) {
            log.error "Exception fetching ${upstreamPos}-th line from delegate", e
            throw e
        }
    }

    private boolean needsDelegateFetch() {
        currentFieldSet == null || position >= currentFieldSet.fieldCount
    }

    // return null to signal end
    protected FieldSet fetchNextDelegateLine() {
        delegate.read()
    }

    /**
     * Can't return null.
     */
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
        cachedValues = (Queue) executionContext.get(
                getExecutionContextKey(SAVED_CACHED_ITEMS))

        if (position > 0) {
            // we restart in the middle of a line
            // discard the context saved line
            currentFieldSet = null
            upstreamPos--
            assert upstreamPos >= 0
        }

        delegate.open(executionContext)
        eagerLineListener?.open(executionContext)

        log.debug "Opened $this, got position $position " +
                "on fieldset $currentFieldSet, cached values $cachedValues"
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
        executionContext.put(
                getExecutionContextKey(SAVED_CACHED_ITEMS),
                cachedValues ? Lists.newLinkedList(cachedValues) : null)

        delegate.update(executionContext)
        eagerLineListener?.update(executionContext)
    }

    @Override
    @SuppressWarnings('CloseWithoutCloseable')
    void close() throws ItemStreamException {
        log.trace("Closing $this")
        upstreamPos = 0
        position = 0
        currentFieldSet = null

        delegate.close()
        eagerLineListener?.close()
    }

    static interface EagerLineListener<T> extends ItemStream {
        /**
         * @param fieldSet - parsed row
         * @param keptItems - items that remained after filtering with {@link this.earlyItemFilter}
         */
        void onLine(FieldSet fieldSet, Collection<T> keptItems)
    }

    static interface EarlyItemFilter<T> {
        boolean keepItem(T item)
    }
}
