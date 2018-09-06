package org.transmartproject.db.multidimquery

import com.google.common.collect.AbstractIterator
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.PeekingIterator
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.transmartproject.core.multidimquery.SortOrder
import org.transmartproject.core.multidimquery.datatable.TableRetrievalParameters
import org.transmartproject.core.multidimquery.DataTable
import org.transmartproject.core.multidimquery.DataTableColumn
import org.transmartproject.core.multidimquery.DataTableRow
import org.transmartproject.core.multidimquery.hypercube.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue

import static java.util.Objects.requireNonNull

@CompileStatic
abstract class AbstractDataTable implements DataTable {

    final Hypercube hypercube
    final ImmutableList<Dimension> rowDimensions, columnDimensions
    final ImmutableMap<Dimension, SortOrder> sort
    final ImmutableMap<Dimension, SortOrder> requestedSort
    // An ordered map, the order is the order in which column dimensions are sorted.
    protected LinkedHashMap<Dimension, SortOrder> columnPriority  // NB: an ordered map
    // A map from dimension to columnDimensions.indexOf(dimension)
    protected Map<Dimension, Integer> columnIndices

    abstract List<DataTableColumnImpl> getColumnKeys()

    /**
     * The total number of rows in this result; only set when known
     */
    Long totalRowCount

    // without an explicit getter the generated getters are final and for some reason PagingDataTable generates overrides
    ImmutableList<Dimension> getRowDimensions() { rowDimensions }
    ImmutableList<Dimension> getColumnDimensions() { columnDimensions }
    ImmutableMap<Dimension, SortOrder> getSort() { sort }
    ImmutableMap<Dimension, SortOrder> getRequestedSort() { requestedSort }

    AbstractDataTable(TableRetrievalParameters args, Hypercube hypercube) {
        requireNonNull this.hypercube = hypercube
        this.rowDimensions = ImmutableList.<Dimension>copyOf((List) args.rowDimensions)
        this.columnDimensions = ImmutableList.<Dimension>copyOf((List) args.columnDimensions)
        this.sort = ImmutableMap.copyOf((Map) args.sort)
        this.requestedSort = ImmutableMap.copyOf(args.userSort)

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


    DataTableColumnImpl newDataTableColumnImpl(List elements, List keys) { new DataTableColumnImpl(elements, keys) }
    @ToString(includes=['keys'], includeNames=true, includePackage=false)
    @EqualsAndHashCode(includes=["keys"])
    class DataTableColumnImpl implements DataTableColumn<DataTableColumnImpl> {
        // elements ant keys are nullable, so they cannot be of type ImmutableList
        final List elements // in the order of columnDimensions
        final List keys

        DataTableColumnImpl(List elements, List keys) {
            this.elements = Collections.unmodifiableList(new ArrayList(elements))
            this.keys = Collections.unmodifiableList(new ArrayList(keys))
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

    @ToString(includes=['offset', 'keys'], includeNames=true, includePackage=false)
    @EqualsAndHashCode(includes=["offset"])
    static class DataTableRowImpl implements DataTableRow<DataTableRowImpl> {
        final long offset
        // elements ant keys are nullable, so they cannot be of type ImmutableList
        final List elements  // in the order of rowDimensions
        final List keys

        DataTableRowImpl(long offset, List elements, List keys) {
            this.offset = offset
            this.elements = Collections.unmodifiableList(new ArrayList(elements))
            this.keys = Collections.unmodifiableList(new ArrayList(keys))
        }

        @Override
        int compareTo(DataTableRowImpl other) {
            offset <=> other.offset  // we defer to the database's order for this one
        }
    }


    RowIterator newRowIterator() { return new RowIterator() }
    class RowIterator extends AbstractIterator<List<HypercubeValue>> {

        PeekingIterator<HypercubeValue> hypercubeIter

        RowIterator() {
            hypercubeIter = hypercube.iterator()
        }

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

        private List rowKey(HypercubeValue cell) {
            List key = []
            for(Dimension d : rowDimensions) {
                key.add(cell.getDimKey(d))
            }
            Collections.unmodifiableList(new ArrayList<>(key))
        }

        private boolean sameGroup(List key, HypercubeValue cell) {
            // if there are no row dimensions, all cells are a separate row
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
