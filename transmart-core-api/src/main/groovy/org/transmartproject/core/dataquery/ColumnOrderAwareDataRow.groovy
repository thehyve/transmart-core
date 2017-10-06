package org.transmartproject.core.dataquery

interface ColumnOrderAwareDataRow<COL extends DataColumn, CELL> extends DataRow<COL, CELL>, Iterable<CELL> {

    CELL getAt(int index)
}
