package org.transmartproject.core.dataquery

interface DataRow<COL extends DataColumn, CELL> {

    String getLabel()

    CELL getAt(COL column)
}
