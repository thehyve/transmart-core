package org.transmartproject.db.dataquery.highdim.metabolite

import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.db.dataquery.highdim.AbstractDataRow

class MetaboliteDataRow extends AbstractDataRow implements BioMarkerDataRow<Object> {

    String hmdbId

    String biochemicalName

    @Override
    String getLabel() {
        biochemicalName
    }

    @Override
    String getBioMarker() {
        hmdbId
    }

}
