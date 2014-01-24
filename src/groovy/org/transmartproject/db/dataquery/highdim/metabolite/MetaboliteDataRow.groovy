package org.transmartproject.db.dataquery.highdim.metabolite

import groovy.transform.ToString
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.db.dataquery.highdim.AbstractDataRow

@ToString(includes=['label', 'bioMarker', 'data'])
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
