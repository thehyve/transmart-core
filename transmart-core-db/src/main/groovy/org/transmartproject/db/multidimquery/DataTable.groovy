package org.transmartproject.db.multidimquery

import com.google.common.collect.AbstractIterator
import com.google.common.collect.HashBasedTable
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.google.common.collect.PeekingIterator
import com.google.common.collect.Table
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.Immutable
import org.springframework.context.annotation.Lazy
import org.transmartproject.core.dataquery.SortOrder
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue

import static java.util.Objects.requireNonNull

@CompileStatic
class DataTable implements Table<DataTableRow, DataTableColumn, HypercubeValue>, AutoCloseable {

    final Hypercube hypercube
    final ImmutableList<Dimension> rowDimensions, columnDimensions
    final ImmutableMap<Dimension, SortOrder> sort
    final long offset
    final int limit

    private PeekingIterator<HypercubeValue> hypercubeIter
    // An ordered map, the order is the order in which column dimensions are sorted.
    private LinkedHashMap<Dimension, SortOrder> columnPriority  // NB: an ordered map
    // A map from dimension to columnDimensions.indexOf(dimension)
    private Map<Dimension, Integer> columnIndices

    @Delegate @Lazy
    Table<DataTableRow, DataTableColumn, HypercubeValue> table = buildTable()

    @Lazy
    List<DataTableRow> rowKeys = rowKeySet().sort(false)

    @Lazy
    List<DataTableColumn> columnKeys = columnKeySet().sort(false)

    /**
     * The total number of rows in this result; only set when known
     */
    Long totalRowCount

    DataTable(Map args, Hypercube hypercube) {
        requireNonNull this.hypercube = hypercube
        this.hypercubeIter = this.hypercube.iterator()
        requireNonNull this.rowDimensions = args.rowDimensions
        requireNonNull this.columnDimensions = args.columnDimensions
        requireNonNull this.sort = args.sort
        this.offset = args.offset ?: 0
        requireNonNull this.limit = args.limit

        columnIndices = columnDimensions.withIndex().collectEntries()

        /**
         * we first derive this from the sort ordering, and if any dimensions are not sorted, from their order in the
         * columnDimensions list. This priority is the grouping that is used within a row. Without any sorting, the
         * priority equals the order of columnDimensions, so grouping happens on the first column dimension first, within
         * that on the second column dimension, and so on. If explicit sorting is specified, that takes priority.
         */
        columnPriority = (LinkedHashMap) sort.findAll { dim, sort -> dim in columnIndices }
        for(def dim : columnDimensions) { columnPriority.putIfAbsent(dim, SortOrder.ASC) }
    }

    private Table<DataTableRow, DataTableColumn, HypercubeValue> buildTable() {
        def deque = new ArrayDeque<List<HypercubeValue>>(limit+1)

        def rowIter = new RowIterator()

        long startOffset = 0
        long toload = offset+limit
        while(toload && rowIter.hasNext()) {
            deque.add(rowIter.next())
            toload--
            if(deque.size() > limit) {
                deque.removeFirst()
                startOffset++
            }
        }
        if(!rowIter.hasNext()) {
            totalRowCount = startOffset + deque.size()
        }

        def rows = Lists.newArrayList(deque)

        Table<DataTableRow, DataTableColumn, HypercubeValue> table = HashBasedTable.create()

        for(int i=0; i<rows.size(); i++) {
            for(def hv : rows[i]) {
                def elems = []
                for(def dim : rowDimensions) {
                    elems.add(hv[dim])
                }
                def row = new DataTableRow(startOffset+i, ImmutableList.copyOf(elems))
                elems.clear()
                for(def dim: columnDimensions) {
                    elems.add(hv[dim])
                }
                def column = new DataTableColumn(this, ImmutableList.copyOf(elems))

                table.put(row, column, hv)
            }
        }
        table
    }

    @Immutable
    @EqualsAndHashCode(includes=["elements"])
    static class DataTableColumn implements Comparable<DataTableColumn> {
        DataTable dt
        ImmutableList elements // in the order of columnDimensions

        @Override
        int compareTo(DataTableColumn other) {
            for (def entry : dt.columnPriority) {
                int idx = dt.columnIndices[entry.key]
                int c = elements[idx] <=> other.elements[idx]
                if (entry.value == SortOrder.DESC) {
                    c = -c
                }
                if(c != 0) return c
            }
            return 0
        }
    }

    @Immutable
    @EqualsAndHashCode(includes=["idx"])
    static class DataTableRow implements Comparable<DataTableRow> {
        long idx
        ImmutableList elements  // in the order of rowDimensions

        int compareTo(DataTableRow other) {
            idx <=> other.idx  // we defer to the database's order for this one
        }
    }

    class RowIterator extends AbstractIterator<List<HypercubeValue>> {

        @Override
        List<HypercubeValue> computeNext() {
            if(!hypercubeIter.hasNext()) return endOfData()

            List key = getKey(hypercubeIter.peek())
            List row = [hypercubeIter.next()]

            while(hypercubeIter.hasNext() && sameGroup(key, hypercubeIter.peek())) {
                row.add(hypercubeIter.next())
            }

            return row
        }

        private ImmutableList getKey(HypercubeValue cell) {
            List key = []
            for(Dimension d : columnDimensions) {
                key.add(cell.getDimKey(d))
            }
            ImmutableList.copyOf(key)
        }

        private boolean sameGroup(List key, HypercubeValue cell) {
            // if there are no column dimensions, all cells are a separate row
            if (!columnDimensions) return false

            for(int i=0; i<columnDimensions.size(); i++) {
                if(key[i] != cell.getDimKey(columnDimensions[i])) {
                    return false
                }
            }
            return true
        }
    }

    @Override void close() {
        hypercube.close()
    }
}
