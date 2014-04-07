package org.transmartproject.db.dataquery

import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult

class InMemoryTabularResult<I extends DataColumn, R extends DataRow> implements TabularResult<I,R> {

    @Delegate
    private TabularResult delegate
    private List<R> internalRows

    InMemoryTabularResult(TabularResult delegate) {
        this.delegate = delegate
        internalRows = delegate.rows.collect()
    }

    @Override
    Iterator<R> iterator() {
        internalRows.iterator()
    }

    @Override
    Iterator<R> getRows() {
        internalRows.iterator()
    }

}
