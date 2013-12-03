package org.transmartproject.core.dataquery

public interface DataRow<COL, CELL> extends Iterable<CELL> {

    String getLabel()

    CELL getAt(int index)

    CELL getAt(COL column)
}
