package org.transmartproject.db.dataquery.highdim.rbm

import groovy.transform.ToString
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.db.dataquery.highdim.AbstractDataRow

@ToString(excludes = [ 'assayIndexMap' ])
class RbmRow extends AbstractDataRow implements BioMarkerDataRow<Object> {

    String uniprotId

    @Override
    String getLabel() {
        uniprotId
    }

    @Override
    String getBioMarker() {
        uniprotId
    }
}
