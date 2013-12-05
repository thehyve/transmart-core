package org.transmartproject.db.dataquery.highdim.protein

import org.transmartproject.db.dataquery.highdim.AbstractBioMarkerDataRow

class ProteinDataRow extends AbstractBioMarkerDataRow {

    String unitProtId

    String peptide

    @Override
    String getLabel() {
        unitProtId
    }

    @Override
    String getBioMarker() {
        unitProtId
    }
}
