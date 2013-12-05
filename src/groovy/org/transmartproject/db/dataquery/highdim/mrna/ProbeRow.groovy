package org.transmartproject.db.dataquery.highdim.mrna

import groovy.transform.ToString
import org.transmartproject.db.dataquery.highdim.AbstractBioMarkerDataRow

@ToString(excludes = [ 'assayIndexMap' ])
class ProbeRow extends AbstractBioMarkerDataRow {

    String probe

    String geneSymbol

    @Override
    String getLabel() {
        probe
    }

    @Override
    String getBioMarker() {
        geneSymbol
    }
}
