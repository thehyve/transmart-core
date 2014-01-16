package jobs.steps

import com.google.common.collect.AbstractIterator
import com.google.common.collect.Iterators
import com.google.common.collect.Maps
import com.google.common.collect.PeekingIterator
import groovy.transform.CompileStatic

@CompileStatic
class ExpandingMapIterator extends AbstractIterator<String[]> {

    ExpandingMapIterator(Iterator<List<Object>> preResults, List<Integer> mapIndexes) {
        if (mapIndexes.empty) {
            throw new IllegalArgumentException('maxIndexes cannot be empty')
        }
        this.preResults = (PeekingIterator) (preResults instanceof PeekingIterator ?
                preResults : Iterators.peekingIterator(preResults))
        setTransformedColumnsIndexes mapIndexes
    }

    private PeekingIterator<List<Object>> preResults

    /* can be shared between calls to computeNext() */
    @Lazy private String[] returnArray = { ->
        assert originalRow != null
        int outputSize = originalRow.size() +
                transformedColumnsIndexes.size()
        new String[outputSize]
    }()

    private List<Object> originalRow

    private List<Set<Map.Entry<String, Object>>> mapIterables

    private List<IteratorWithCurrent<Map.Entry<String, Object>>> mapIterators

    private Integer maxRank

    /* original index -> new index */
    private Map<Integer, Integer> transformedColumnsIndexes = Maps.newTreeMap()

    private void setTransformedColumnsIndexes(List<Integer> indexes) {
        int count = 0
        indexes.each { Integer it ->
            transformedColumnsIndexes[it] = it + count++
        }
    }

    @Override
    protected String[] computeNext() {
        if (originalRow == null) {
            // no row read yet or previous one already exhausted
            if (!readOriginalRow()) {
                return null /* we got to the end, endOfData() was called */
            }
        }

        nextGeneratedRow()
    }

    private boolean readOriginalRow() { // false if nothing else to read
        while (true) {
            if (!preResults.hasNext()) {
                endOfData()
                return false
            }

            originalRow = preResults.next()
            prefillUnchangedColumns()

            if (resetIterables()) {
                return true
            } else {
                // one or more iterables are empty; no rows will be generated
                // for this original row
                //continue
            }
        }
    }

    private void prefillUnchangedColumns() {
        Iterator nextTransformedIterator = Iterators.concat(
                transformedColumnsIndexes.keySet().iterator(),
                [Integer.MAX_VALUE].iterator()) // original indexes of transformed cols
        def nextTransformed = nextTransformedIterator.next()

        def writingIndex = 0
        originalRow.eachWithIndex { value, index ->
            if (index == nextTransformed) {
                writingIndex += 2
                nextTransformed = nextTransformedIterator.next()
            } else {
                returnArray[writingIndex++] = value as String
            }
        }
    }

    private boolean resetIterables() { // false if any iterable is empty
        mapIterables = transformedColumnsIndexes.keySet().collect { Integer index ->
            /* otherwise the column gave a map on the first row but not on
             * a subsequent one, which shouldn't happen */
            assert originalRow[index] instanceof Map

            ((Map) originalRow[index]).entrySet()
        }

        mapIterators = (List) mapIterables*.iterator().collect { Iterator it ->
            new IteratorWithCurrent(inner: it)
        }

        try {
            mapIterators.each {
                Iterator it -> it.next()
            }
        } catch (NoSuchElementException nse) {
            return false // one of the iterators is empty
        }

        maxRank = mapIterables.size() - 1
        true
    }

    private IteratorWithCurrent createIterator(int rank) {
        def r = new IteratorWithCurrent(inner: mapIterables[rank].iterator())
        r.next()
        r
    }

    private String[] nextGeneratedRow() {
        int i = 0
        transformedColumnsIndexes.values().each { Integer newColumnIndex ->
            Map.Entry<String, Object> entry = mapIterators[i++].current

            assert entry != null

            returnArray[newColumnIndex] = entry.value
            returnArray[newColumnIndex + 1] = entry.key
        }

        /* move forward */
        def currentRank = maxRank
        while (!mapIterators[currentRank].hasNext()) {
            if (currentRank == 0) {
                originalRow = null /* everything seen there */
                return returnArray
            }
            mapIterators[currentRank] = createIterator currentRank
            currentRank--
        }
        mapIterators[currentRank].next()

        return returnArray
    }

    static class IteratorWithCurrent<T> implements Iterator {

        Iterator<T> inner

        T current

        @Override
        boolean hasNext() {
            inner.hasNext()
        }

        @Override
        T next() {
            current = inner.next()
        }

        @Override
        void remove() {
            inner.remove()
        }
    }
}
