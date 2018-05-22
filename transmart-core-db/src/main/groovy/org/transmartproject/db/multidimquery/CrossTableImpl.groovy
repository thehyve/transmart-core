package org.transmartproject.db.multidimquery

import com.google.common.collect.ImmutableList
import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.CrossTable
import org.transmartproject.core.multidimquery.CrossTableRow

@CompileStatic
class CrossTableImpl implements CrossTable {
    final ImmutableList<CrossTableRow> rows

    CrossTableImpl(List rows){
        this.rows = ImmutableList.copyOf(rows)
    }

    static class CrossTableRowImpl implements CrossTableRow {
        final ImmutableList counts

        CrossTableRowImpl(List counts) {
            this.counts = ImmutableList.copyOf(counts)
        }
    }
}
