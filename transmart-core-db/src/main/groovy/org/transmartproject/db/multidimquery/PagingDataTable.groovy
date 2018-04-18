package org.transmartproject.db.multidimquery

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Lists
import com.google.common.collect.Table
import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.DataTable
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue

import static java.util.Objects.requireNonNull

@CompileStatic
class PagingDataTable extends AbstractDataTable implements DataTable {

    final long offset
    final int limit

    @Lazy @Delegate
    Table<DataTableRowImpl, DataTableColumnImpl, HypercubeValue> table = buildTable()

    @Lazy
    List<DataTableRowImpl> rowKeys = rowKeySet().sort(false)

    @Lazy
    List<DataTableColumnImpl> columnKeys = columnKeySet().sort(false)

    // The @Lazy somehow fails to generate these bridge methods
    Map<DataTableRowImpl, HypercubeValue> column(columnKey) { table.column((DataTableColumnImpl) columnKey) }
    Map<DataTableColumnImpl, HypercubeValue> row(rowKey) { table.row((DataTableRowImpl) rowKey) }
    HypercubeValue put(rowKey, columnKey, value) {
        table.put((DataTableRowImpl) rowKey, (DataTableColumnImpl) columnKey, (HypercubeValue) value)
    }

    PagingDataTable(Map args, Hypercube hypercube) {
        super(args, hypercube)

        this.offset = (long) (args.offset ?: 0)
        requireNonNull this.limit = (int) args.limit
    }

    private Table<DataTableRowImpl, DataTableColumnImpl, HypercubeValue> buildTable() {
        def deque = new ArrayDeque<List<HypercubeValue>>(limit+1)

        def rowIter = newRowIterator()

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
                    keys.add(hv.getDimKey(dim))
                }
                def row = new DataTableRowImpl(startOffset+i, elems, keys)
                elems.clear()
                keys.clear()
                for(def dim: columnDimensions) {
                    elems.add(hv[dim])
                    keys.add(hv.getDimKey(dim))
                }
                def column = newDataTableColumnImpl(elems, keys)

                table.put(row, column, hv)
            }
        }
        table
    }
}
