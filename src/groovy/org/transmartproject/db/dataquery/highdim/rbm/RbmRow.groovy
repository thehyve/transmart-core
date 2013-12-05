package org.transmartproject.db.dataquery.highdim.rbm

import groovy.transform.ToString
import org.transmartproject.db.dataquery.highdim.AbstractBioMarkerDataRow
import org.transmartproject.db.dataquery.highdim.AbstractDataRow

@ToString(excludes = [ 'assayIndexMap' ])
class RbmRow extends AbstractBioMarkerDataRow {

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
