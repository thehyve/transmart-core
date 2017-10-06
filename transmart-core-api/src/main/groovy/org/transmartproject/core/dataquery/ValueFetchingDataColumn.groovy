package org.transmartproject.core.dataquery

interface ValueFetchingDataColumn<T, R extends DataRow> extends DataColumn {

    /**
     * A value in this column for the given row.
     *
     * @return value
     */
    T getValue(R row)
}
