package org.transmartproject.db.dataquery.highdim.rnaseqcog

import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.db.dataquery.highdim.AbstractDataRow

class RnaSeqCogDataRow extends AbstractDataRow implements BioMarkerDataRow<Object> {

    String transcriptId

    String gene

    @Override
    String getLabel() {
        transcriptId
    }

    @Override
    String getBioMarker() {
        gene
    }
}
