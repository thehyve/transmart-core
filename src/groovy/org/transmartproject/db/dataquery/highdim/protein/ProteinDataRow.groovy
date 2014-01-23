package org.transmartproject.db.dataquery.highdim.protein

import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.db.dataquery.highdim.AbstractDataRow

class ProteinDataRow extends AbstractDataRow implements BioMarkerDataRow<Object> {

    String unitProtId

    String peptide

    @Override
    String getLabel() {
        peptide
    }

    @Override
    String getBioMarker() {
        unitProtId
    }

}
