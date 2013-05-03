package org.transmartproject.core.dataquery

/**
 * The result of a data query that provides a list of indices (columns) that
 * can be used to fetch individual values from the rows,
 * which are also provided through an {@link Iterable}.
 *
 * @param < I > The type for the row indexes
 * @param < R > The type for the rows themselves
 */
public interface DataQueryResult<I, R> extends Closeable {

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
     * @return the typed result set rows
     */
    Iterator<R> getRows()
}
