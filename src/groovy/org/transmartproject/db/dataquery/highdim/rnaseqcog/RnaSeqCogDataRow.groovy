package org.transmartproject.db.dataquery.highdim.rnaseqcog

import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.db.dataquery.highdim.AbstractDataRow

class RnaSeqCogDataRow extends AbstractDataRow implements BioMarkerDataRow<Object> {

    String annotationId

    String geneSymbol

    String geneId

    @Override
    String getLabel() {
        annotationId
    }

    @Override
    String getBioMarker() {
        geneSymbol
    }

}
