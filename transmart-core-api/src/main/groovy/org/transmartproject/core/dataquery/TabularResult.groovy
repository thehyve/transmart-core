package org.transmartproject.core.dataquery

import org.transmartproject.core.IterableResult

/**
 * The result of a data query that provides a list of indices (columns) that
 * can be used to fetch individual values from the rows,
 * which are also provided through an {@link Iterable}.
 *
 * @param < I > The type for the row indexes
 * @param < R > The type for the rows themselves
 */
interface TabularResult<I extends DataColumn, R extends DataRow> extends IterableResult<R> {

    /**
     * Used to obtain the "columns" of the result set' the indices used to
     * obtain specific values from the rows.
     *
     * @return a typed list of indices present in all the rows
     */
    List<I> getIndicesList()

    /**
     * The iterator result set, organized as rows, which is a map of values
     * indexed by the indices returned by {@link #getIndicesList()}.
     *
     * Same as {@link Iterable#iterator()}.
     *
     * @return the typed result set rows
     */
    Iterator<R> getRows()

    String getColumnsDimensionLabel()

    String getRowsDimensionLabel()
}
