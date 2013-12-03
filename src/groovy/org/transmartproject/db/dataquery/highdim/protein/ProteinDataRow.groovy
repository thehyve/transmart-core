package org.transmartproject.db.dataquery.highdim.protein

import org.transmartproject.db.dataquery.highdim.AbstractDataRow

class ProteinDataRow extends AbstractDataRow {

    String unitProtId

    String peptide

    @Override
    String getLabel() {
        unitProtId
    }
}
