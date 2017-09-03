/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.dataquery

import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.dataquery.ColumnOrderAwareDataRow
import org.transmartproject.core.dataquery.TabularResult

class MockTabularResult<I extends DataColumn, R extends ColumnOrderAwareDataRow> implements TabularResult<I,R> {

    String columnsDimensionLabel = "column label"
    String rowsDimensionLabel = "row label"
    Collection<R> rowsList
    List<I> indicesList

    Iterator<R> getRows() {
        rowsList.iterator()
    }

    Iterator<R> iterator() { rows }

    void close() {}
}
