package org.transmartproject.db.dataquery.highdim.mrna

import groovy.transform.ToString
import org.transmartproject.db.dataquery.highdim.AbstractDataRow

@ToString(excludes = [ 'assayIndexMap' ])
class ProbeRow extends AbstractDataRow {

    String probe

    String geneSymbol

    String organism

    @Override
    String getLabel() {
        probe
    }
}
