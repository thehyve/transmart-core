package org.transmartproject.core.multidimquery

import com.google.common.collect.Table
import org.transmartproject.core.dataquery.SortOrder

/**
 * A DataTable is a two-dimensional representation of a hypercube. Which dimensions are represented as rows or as
 * columns is selectable.
 */
interface DataTable extends Table<DataTableRow, DataTableColumn, HypercubeValue> {

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

    /**
     * @return the table column headers in order
     */
    List<DataTableColumn> getColumnKeys()

    /**
     * @return the sorting that was actually used for the hypercube from which this datatable was constructed
     */
    Map<Dimension, SortOrder> getSort()
}

interface DataTableRow<SELF extends DataTableRow<SELF>> extends Comparable<SELF> {
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