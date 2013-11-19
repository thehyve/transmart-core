package org.transmartproject.core.dataquery

/**
 * The result of a data query that provides a list of indices (columns) that
 * can be used to fetch individual values from the rows,
 * which are also provided through an {@link Iterable}.
 *
 * @param < I > The type for the row indexes
 * @param < R > The type for the rows themselves
 *
 * @deprecated To be removed when aCGH is ported to new interface
 */
@Deprecated
public interface DataQueryResult<I, R> extends Closeable {

    /**
     * Used to obtain the "columns" of the result set' the indices used to
     * obtain specific values from the rows.
     *
     * @return a typed list of indices present in all the rows
     */
    @Deprecated
    List<I> getIndicesList()

    /**
     * The iterator result set, organized as rows, which is a map of values
     * indexed by the indices returned by {@link #getIndicesList()}.
     *
     * @return the typed result set rows
     */
    @Deprecated
    Iterator<R> getRows()
}
