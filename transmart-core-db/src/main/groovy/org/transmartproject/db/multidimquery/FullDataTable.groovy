package org.transmartproject.db.multidimquery

import com.google.common.collect.AbstractIterator
import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableList
import com.google.common.collect.Multimap
import groovy.transform.CompileStatic
import org.transmartproject.core.IterableResult
import org.transmartproject.core.multidimquery.DataTable
import org.transmartproject.core.multidimquery.DataTableColumn
import org.transmartproject.core.multidimquery.DataTableRow
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.core.multidimquery.StreamingDataTable

@CompileStatic
class FullDataTable extends AbstractDataTable implements StreamingDataTable {

    final ImmutableList<DataTableColumnImpl> columnKeys

    // maps of elementKey -> element for each dimension.
    private Map<Dimension, Map> elementsMap

    FullDataTable(Map args, Hypercube hypercube) {
        super(args, hypercube)

        columnKeys = buildColumns(hypercube)
    }

    ImmutableList<DataTableColumnImpl> buildColumns(Hypercube hypercube) {
        // We collect the columns by looping through the result twice. A more efficient way would be to change the
        // first query to return only unique combinations of column dimension keys, but that requires changes in the
        // query building code. That would also require changes here in the loading of row elements.

        // This loop runs for each hypercube value, so for performance don't use closures here
        Set<DataTableColumnImpl> columns = [] as Set
        Multimap<Dimension, Object> rowKeys = HashMultimap.create()
        for(def val : hypercube) {
            List keys = []
            for(def dim : columnDimensions) {
                keys.add val.getDimKey(dim)
            }
            columns.add(newDataTableColumnImpl([], keys))
            for(def dim : rowDimensions) {
                rowKeys.put(dim, val.getDimKey(dim))
            }
        }

        List<DataTableColumnImpl> sortedColumns = columns.sort(false)

        // Note: we initialise the object property here
        elementsMap = (Map) columnIndices.collectEntries { Dimension dim, Integer idx ->
            def keys = sortedColumns.collect(new LinkedHashSet(), (Closure) /*Cast to please the compiler*/ {
                DataTableColumnImpl col -> col.keys[idx] }).toList()
            def elements = dim.resolveElements(keys)
            [dim, [keys, elements].transpose().collectEntries()]
        }

        rowKeys.asMap().each { d, k ->
            Dimension dim = (Dimension) d
            List keys = k as List
            def elements = dim.resolveElements(keys)
            elementsMap[dim] = [keys, elements].transpose().collectEntries()
        }

        List<DataTableColumnImpl> finalColumns = sortedColumns.collect { col ->
            newDataTableColumnImpl(columnIndices.collect { Dimension dim, Integer idx ->
                elementsMap[dim][col.keys[idx]]
            } , col.keys)
        }

        ImmutableList.copyOf(finalColumns)
    }

    DataTableColumnImpl getColumn(HypercubeValue hv) {
        List elems = []
        List keys = []
        for(def dim: columnDimensions) {
            def key = hv.getDimKey(dim)
            elems.add(elementsMap[dim][key])
            keys.add(key)
        }
        newDataTableColumnImpl(elems, keys)
    }

    DataTableRowImpl getRow(HypercubeValue hv, long offset) {
        List elems = []
        List keys = []
        for(def dim: rowDimensions) {
            def key = hv.getDimKey(dim)
            elems.add(elementsMap[dim][key])
            keys.add(key)
        }
        new DataTableRowImpl(offset, elems, keys)
    }

    Iterator<FullDataTableRow> getRows() {
        return new DataTableRowsIterator()
    }

    Iterator<FullDataTableRow> iterator() {
        getRows()
    }

    class FullDataTableRow {

        DataTableRow rowHeader
        Map<DataTableColumn, HypercubeValue> rowValues = [:]

        FullDataTableRow(List<HypercubeValue> row, long offset) {
            this.rowHeader = getRow(row[0], offset)
            for(def hv : row) {
                this.rowValues[getColumn(hv)] = hv
            }
        }

        List headerValues() {
            List vals = []
            for(int i=0; i<rowDimensions.size(); i++) {
                vals.add(rowDimensions[i].getKey(rowHeader.elements[i]))
            }
            vals
        }

        List dataValues() {
            List vals = []
            for(def col : columnKeys) {
                vals.add(getAt(col))
            }
            vals
        }

        HypercubeValue getAt(DataTableColumn column) {
            rowValues[column]
        }

        def getAtValue(DataTableColumn column) {
            getAt(column)?.value
        }
    }

    class DataTableRowsIterator extends AbstractIterator<FullDataTableRow> {

        Iterator<List<HypercubeValue>> rowIterator = newRowIterator()
        long offset = 0

        @Override
        FullDataTableRow computeNext() {
            if(!rowIterator.hasNext()) return endOfData()

            new FullDataTableRow(rowIterator.next(), offset++)
        }
    }
}
