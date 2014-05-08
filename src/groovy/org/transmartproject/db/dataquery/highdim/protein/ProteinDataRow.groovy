package org.transmartproject.db.dataquery.highdim.protein

import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.db.dataquery.highdim.AbstractDataRow

class ProteinDataRow extends AbstractDataRow implements BioMarkerDataRow<Object> {

    String uniprotName

    String peptide

    @Override
    String getLabel() {
        peptide
    }

    @Override
    String getBioMarker() {
        uniprotName
    }

    @Override
    public String toString() {
        com.google.common.base.Objects.toStringHelper(this)
                .add('data', data).toString()
    }
}
