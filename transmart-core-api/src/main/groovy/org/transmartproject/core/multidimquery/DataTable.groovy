package org.transmartproject.core.multidimquery

import com.google.common.collect.Multimap
import com.google.common.collect.Table
import org.transmartproject.core.IterableResult
import org.transmartproject.core.multidimquery.hypercube.Dimension

/**
 * A DataTable is a two-dimensional representation of a hypercube. Which dimensions are represented as rows or as
 * columns is selectable.
 */
interface DataTable extends AutoCloseable, Closeable {

    /**
     * @return the underlying hypercube
     */
    Hypercube getHypercube()

    /**
     * The list of row dimensions in order
     */
    List<Dimension> getRowDimensions()

    /**
     * The list of column dimensions in order
     */
    List<Dimension> getColumnDimensions()

    /**
     * @return the table column headers in order
     */
    List<DataTableColumn> getColumnKeys()

    /**
     * @return the sorting that was actually used for the hypercube from which this datatable was constructed. The
     * map is an ordered map.
     */
    Map<Dimension, SortOrder> getSort()

    /**
     * @return the sorting that was requested by the user. The actual sorting used may be more elaborate, see
     * getSort(). The returned map is an ordered map.
     */
    Map<Dimension, SortOrder> getRequestedSort()
}

/**
 * PagingDataTable represents an in-memory view on a data table that contains a subset of all rows
 */
interface PagingDataTable extends DataTable, Table<DataTableRow, DataTableColumn, ? extends Collection<HypercubeValue>> {
    /**
     * The row offset of the first row of this data table
     */
    long getOffset()

    /**
     * @return the number of rows in this data table (i.e. one page)
     */
    int getLimit()

    /**
     * @return the total number of rows in this query result (without pagination), if known. If not known this
     * returns null.
     */
    Long getTotalRowCount()

    /**
     * @return The table row headers in order
     */
    List<DataTableRow> getRowKeys()
}

/**
 * StreamingDataTable represents a DataTable that is not all loaded in memory, but the rows can be streamed.
 */
interface StreamingDataTable extends DataTable, IterableResult<FullDataTableRow> {}

interface FullDataTableRow {
    DataTableRow getRowHeader()

    Set<DataTableColumn> getColumnKeys()

    Multimap<DataTableColumn, HypercubeValue> getMultimap()

    List getHeaderValues()
    List<Collection> getDataValues()
}

interface DataTableRow<SELF extends DataTableRow<SELF>>
        extends Comparable<SELF> {
    /**
     * @return a list with the elements of the row dimensions for this row
     */
    List getElements()

    /**
     * The (unpaged) row offset
     * @return
     */
    long getOffset()
}

interface DataTableColumn<SELF extends DataTableColumn<SELF>> extends Comparable<SELF> {
    /**
     * @return a list with the elements of the column dimensions for this column
     */
    List getElements()
}
