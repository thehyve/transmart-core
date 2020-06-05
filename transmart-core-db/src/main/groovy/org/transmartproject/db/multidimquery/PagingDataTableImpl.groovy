package org.transmartproject.db.multidimquery

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Lists
import com.google.common.collect.Table
import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.datatable.PaginationParameters
import org.transmartproject.core.multidimquery.datatable.TableRetrievalParameters
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.core.multidimquery.PagingDataTable

import static java.util.Objects.requireNonNull

@CompileStatic
class PagingDataTableImpl extends AbstractDataTable implements PagingDataTable {

    final long offset
    final int limit

    @Lazy @Delegate
    Table<DataTableRowImpl, DataTableColumnImpl, ? extends Collection<HypercubeValue>> table = buildTable()

    @Lazy
    List<DataTableRowImpl> rowKeys = rowKeySet().sort(false)

    @Lazy
    List<DataTableColumnImpl> columnKeys = columnKeySet().sort(false)

    // The @Lazy somehow fails to generate these bridge methods
    Map<DataTableRowImpl, ? extends Collection<HypercubeValue>> column(columnKey) { table.column((DataTableColumnImpl) columnKey) }
    Map<DataTableColumnImpl, ? extends Collection<HypercubeValue>> row(rowKey) { table.row((DataTableRowImpl) rowKey) }
    Collection<HypercubeValue> put(rowKey, columnKey, value) {
        table.put((DataTableRowImpl) rowKey, (DataTableColumnImpl) columnKey, (Collection<HypercubeValue>) value)
    }
    //Map<DataTableColumnImpl, ? extends Collection<HypercubeValue>> row(DataTableRowImpl row) { table.row(row) }

    PagingDataTableImpl(TableRetrievalParameters args, PaginationParameters pagination, Hypercube hypercube) {
        super(args, hypercube)

        this.offset = (long) (pagination.offset ?: 0)
        requireNonNull this.limit = (int) pagination.limit
    }

    private Table<DataTableRowImpl, DataTableColumnImpl, Collection<HypercubeValue>> buildTable() {
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

        Table<DataTableRowImpl, DataTableColumnImpl, List<HypercubeValue>> table = HashBasedTable.create()

        for (int i=0; i<rows.size(); i++) {
            def firstHv = rows[i][0]
            if (firstHv == null) continue
            List elems = []
            List keys = []
            for (def dim : rowDimensions) {
                elems.add(firstHv[dim])
                keys.add(firstHv.getDimKey(dim))
            }
            def rowHeader = new DataTableRowImpl(startOffset+i, elems, keys)

            def row = table.row(rowHeader)

            for (def hv : rows[i]) {
                elems.clear()
                keys.clear()
                for (def dim: columnDimensions) {
                    elems.add(hv[dim])
                    keys.add(hv.getDimKey(dim))
                }
                def column = newDataTableColumnImpl(elems, keys)

                List vals = (List) row[column]
                if (vals == null) {
                    vals = row[column] = []
                }
                vals.add(hv)
            }
        }
        (Table) table
    }
}
