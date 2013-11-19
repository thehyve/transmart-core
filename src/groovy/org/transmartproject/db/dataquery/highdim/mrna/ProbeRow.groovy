package org.transmartproject.db.dataquery.highdim.mrna

import groovy.transform.ToString
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.highdim.AssayColumn

@ToString(excludes = [ 'assayIndexMap' ])
class ProbeRow implements DataRow<AssayColumn, Object> {

    String probe

    String geneSymbol

    String organism

    Map<AssayColumn, Integer> assayIndexMap

    List<Object> data

    @Override
    String getLabel() {
        probe
    }

    @Override
    Object getAt(int index) {
        data[index]
    }

    @Override
    Object getAt(AssayColumn column) {
        data[assayIndexMap[column]]
    }
}
