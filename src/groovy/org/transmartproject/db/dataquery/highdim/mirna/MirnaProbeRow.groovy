package org.transmartproject.db.dataquery.highdim.mirna

import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.db.dataquery.highdim.AbstractDataRow

class MirnaProbeRow extends AbstractDataRow implements BioMarkerDataRow<Object> {

    String label

    String bioMarker /* the miR id */

}
