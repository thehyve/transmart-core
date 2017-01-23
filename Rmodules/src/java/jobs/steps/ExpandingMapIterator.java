package jobs.steps;

import com.google.common.collect.*;

import java.util.*;

public class ExpandingMapIterator extends AbstractIterator<String[]> {

    public ExpandingMapIterator(Iterator<List<Object>> preResults, List<Integer> mapIndexes) {
        this(preResults, mapIndexes, 1);
    }

    protected ExpandingMapIterator(Iterator<List<Object>> preResults,
                                   List<Integer> mapIndexes,
                                   int numberOfNewRowsPerMapColumn) {
        this.numberOfNewRowsPerMapColumn = numberOfNewRowsPerMapColumn;
        if (mapIndexes.isEmpty()) {
            throw new IllegalArgumentException("maxIndexes cannot be empty");
        }
        this.preResults = (PeekingIterator) (preResults instanceof PeekingIterator ?
                preResults : Iterators.peekingIterator(preResults));
        setTransformedColumnsIndexes(mapIndexes);
    }
    
    private PeekingIterator<List<Object>> preResults;

    /* we don't support expanding different columns to
     * different numbers of extra columns */
    protected int numberOfNewRowsPerMapColumn = 1;

    /* can be shared between calls to computeNext() */
    private String[] _returnArray;

    protected String[] getReturnArray() {
        assert originalRow != null;

        if (_returnArray == null) {
            int outputSize = originalRow.size() +
                    transformedColumnsMap.size() * numberOfNewRowsPerMapColumn;
            _returnArray = new String[outputSize];
        }

        return _returnArray;
    }

    private List<Object> originalRow;

    private List<Set<Map.Entry<String, Object>>> mapIterables;

    private List<IteratorWithCurrent<Map.Entry<String, Object>>> mapIterators;

    private Integer maxRank;

    /* original index -> new index */
    private Map<Integer, Integer> transformedColumnsMap = Maps.newTreeMap();

    private void setTransformedColumnsIndexes(List<Integer> indexes) {
        int count = 0;
        for (Integer i: indexes) {
            transformedColumnsMap.put(i,
                    i + (count++ * numberOfNewRowsPerMapColumn));
        }
    }

    @Override
    protected String[] computeNext() {
        if (originalRow == null) {
            // no row read yet or previous one already exhausted;
            if (!readOriginalRow()) {
                return null; /* we got to the end, endOfData() was called */
            }
        }

        return nextGeneratedRow();
    }

    private boolean readOriginalRow() { // false if nothing else to read
        while (preResults.hasNext()) {
            originalRow = preResults.next();

            if (resetIterables()) {
                prefillUnchangedColumns();
                return true;
            } else {
                // one or more iterables are empty; no rows will be generated
                // for this original row
                //continue
            }
        }

        // no more results;
        endOfData();
        return false;
    }

    private void prefillUnchangedColumns() {
        // original indexes of transformed cols
        Iterator<Integer> nextTransformedIterator = Iterators.concat(
                transformedColumnsMap.keySet().iterator(),
                Iterators.forArray(new Integer[] {Integer.MAX_VALUE}));
        Integer nextTransformed = nextTransformedIterator.next();

        int writingIndex = 0;
        for (int index = 0; index < originalRow.size(); index++) {
            Object value = originalRow.get(index);
            if (index == nextTransformed) {
                writingIndex += numberOfNewRowsPerMapColumn + 1;
                nextTransformed = nextTransformedIterator.next();
            } else {
                getReturnArray()[writingIndex++] = value.toString();
            }
        }
    }

    private boolean resetIterables() { // false if any iterable is empty
        Set<Integer> originalIndexes = transformedColumnsMap.keySet();

        mapIterables = Lists.newArrayListWithCapacity(originalIndexes.size());
        for (int index: originalIndexes) {
            assert originalRow.get(index) instanceof Map;
            mapIterables.add(((Map) originalRow.get(index)).entrySet());
        }

        mapIterators = Lists.newArrayListWithCapacity(mapIterables.size());
        for (Iterable<Map.Entry<String, Object>> iterable: mapIterables) {
            IteratorWithCurrent iteratorWithCurrent = new IteratorWithCurrent();
            iteratorWithCurrent.inner = iterable.iterator();
            mapIterators.add(iteratorWithCurrent);
        }

        try {
            for (IteratorWithCurrent<Map.Entry<String, Object>> iterator: mapIterators) {
                iterator.next();
            }
        } catch (NoSuchElementException nse) {
            return false; // one of the iterators is empty
        }

        maxRank = mapIterables.size() - 1;
        return true;
    }

    private IteratorWithCurrent<Map.Entry<String, Object>> createIterator(int rank) {
        IteratorWithCurrent<Map.Entry<String, Object>> r =
                new IteratorWithCurrent<Map.Entry<String, Object>>();
        r.inner = mapIterables.get(rank).iterator();
        r.next();
        return r;
    }

    protected void writeEntry(Map.Entry<String, Object> entry, int index) {
        getReturnArray()[index] = entry.getValue().toString();
        getReturnArray()[index + 1] = entry.getKey();
    }

    private String[] nextGeneratedRow() {
        int i = 0;
        for (Integer newColumnIndex: transformedColumnsMap.values()) {
            Map.Entry<String, Object> entry = mapIterators.get(i++).current;

            assert entry != null;

            writeEntry(entry, newColumnIndex);
        }

        /* move forward */
        Integer currentRank = maxRank;
        while (!mapIterators.get(currentRank).hasNext()) {
            if (currentRank == 0) {
                originalRow = null /* everything seen there */;
                return getReturnArray();
            }
            mapIterators.set(currentRank, createIterator(currentRank));
            currentRank--;
        }
        mapIterators.get(currentRank).next();

        return getReturnArray();
    }

    public static class IteratorWithCurrent<T> implements Iterator<T> {

        Iterator<T> inner;

        T current;

        @Override
        public boolean hasNext() {
            return inner.hasNext();
        }

        @Override
        public T next() {
            return (current = inner.next());
        }

        @Override
        public void remove() {
            inner.remove();
        }
    }
}
