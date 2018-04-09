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
import org.transmartproject.core.dataquery.SortOrder
import org.transmartproject.core.multidimquery.DataTable
import org.transmartproject.core.multidimquery.DataTableColumn
import org.transmartproject.core.multidimquery.DataTableRow
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue

import static java.util.Objects.requireNonNull

@CompileStatic
class DataTableImpl implements DataTable {

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

    @Lazy @Delegate
    Table<DataTableRowImpl, DataTableColumnImpl, HypercubeValue> table = buildTable()

    @Lazy
    List<DataTableRowImpl> rowKeys = rowKeySet().sort(false)

    @Lazy
    List<DataTableColumnImpl> columnKeys = columnKeySet().sort(false)

    /**
     * The total number of rows in this result; only set when known
     */
    Long totalRowCount

    // The @Lazy somehow fails to generate these bridge methods
    Map<DataTableRowImpl, HypercubeValue> column(columnKey) { table.column((DataTableColumnImpl) columnKey) }
    Map<DataTableColumnImpl, HypercubeValue> row(rowKey) { table.row((DataTableRowImpl) rowKey) }
    HypercubeValue put(rowKey, columnKey, value) {
        table.put((DataTableRowImpl) rowKey, (DataTableColumnImpl) columnKey, (HypercubeValue) value)
    }

    DataTableImpl(Map args, Hypercube hypercube) {
        requireNonNull this.hypercube = hypercube
        this.hypercubeIter = this.hypercube.iterator()
        requireNonNull this.rowDimensions = ImmutableList.<Dimension>copyOf((List) args.rowDimensions)
        requireNonNull this.columnDimensions = ImmutableList.<Dimension>copyOf((List) args.columnDimensions)
        requireNonNull this.sort = ImmutableMap.copyOf((Map) args.sort)
        this.offset = (long) (args.offset ?: 0)
        requireNonNull this.limit = (int) args.limit

        columnIndices = columnDimensions.withIndex().collectEntries()

        /**
         * we first derive this from the sort ordering, and if any dimensions are not sorted, from their order in the
         * columnDimensions list. This priority is the grouping that is used within a row. Without any sorting, the
         * priority equals the order of columnDimensions, so grouping happens on the first column dimension first, within
         * that on the second column dimension, and so on. If explicit sorting is specified, that takes priority.
         */
        columnPriority = (LinkedHashMap) sort.findAll { dim, sort ->
            dim in columnIndices.keySet() // `dim in columnIndices` does not work as expected if columnIndices[dim] == 0
        }
        for(def dim : columnDimensions) { columnPriority.putIfAbsent(dim, SortOrder.ASC) }
    }

    private Table<DataTableRowImpl, DataTableColumnImpl, HypercubeValue> buildTable() {
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

        Table<DataTableRowImpl, DataTableColumnImpl, HypercubeValue> table = HashBasedTable.create()

        for(int i=0; i<rows.size(); i++) {
            for(def hv : rows[i]) {
                List elems = []
                List keys = []
                for(def dim : rowDimensions) {
                    elems.add(hv[dim])
                }
                def row = new DataTableRowImpl(startOffset+i, ImmutableList.copyOf(elems))
                elems.clear()
                for(def dim: columnDimensions) {
                    elems.add(hv[dim])
                    keys.add(hv.getDimKey(dim))
                }
                def column = new DataTableColumnImpl(elems, keys)

                table.put(row, column, hv)
            }
        }
        table
    }

    @EqualsAndHashCode(includes=["keys"])
    class DataTableColumnImpl implements DataTableColumn<DataTableColumnImpl> {
        final ImmutableList elements // in the order of columnDimensions
        final ImmutableList keys

        DataTableColumnImpl(List elements, List keys) {
            this.elements = ImmutableList.copyOf(elements)
            this.keys = ImmutableList.copyOf(keys)
        }

        @Override
        int compareTo(DataTableColumnImpl other) {
            for (def entry : columnPriority) {
                int idx = columnIndices[entry.key]
                int c = compareKeys(keys[idx], other.keys[idx])
                if (entry.value == SortOrder.DESC) {
                    c = -c
                }
                if(c != 0) return c
            }
            return 0
        }

        static int compareKeys(e1, e2) {
            if(e1 == null) (e2 == null ? 0 : -1)
            else if(e2 == null) 1
            ((Comparable) e1) <=> e2
        }
    }

    @Immutable(knownImmutableClasses=[ImmutableList])
    @EqualsAndHashCode(includes=["offset"])
    static class DataTableRowImpl implements DataTableRow<DataTableRowImpl> {
        long offset
        ImmutableList elements  // in the order of rowDimensions

        @Override
        int compareTo(DataTableRowImpl other) {
            offset <=> other.offset  // we defer to the database's order for this one
        }
    }

    class RowIterator extends AbstractIterator<List<HypercubeValue>> {

        @Override
        List<HypercubeValue> computeNext() {
            if(!hypercubeIter.hasNext()) return endOfData()

            List key = rowKey(hypercubeIter.peek())
            List row = [hypercubeIter.next()]

            while(hypercubeIter.hasNext() && sameGroup(key, hypercubeIter.peek())) {
                row.add(hypercubeIter.next())
            }

            return row
        }

        private ImmutableList rowKey(HypercubeValue cell) {
            List key = []
            for(Dimension d : rowDimensions) {
                key.add(cell.getDimKey(d))
            }
            ImmutableList.copyOf(key)
        }

        private boolean sameGroup(List key, HypercubeValue cell) {
            // if there are no column dimensions, all cells are a separate row
            if (!rowDimensions) return false

            for(int i=0; i<rowDimensions.size(); i++) {
                if(key[i] != cell.getDimKey(rowDimensions[i])) {
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
