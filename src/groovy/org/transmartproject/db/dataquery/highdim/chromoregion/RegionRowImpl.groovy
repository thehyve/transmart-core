package org.transmartproject.db.dataquery.highdim.chromoregion

import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.chromoregion.RegionRow
import org.transmartproject.db.dataquery.highdim.AbstractDataRow

final class RegionRowImpl extends AbstractDataRow implements RegionRow, BioMarkerDataRow {

    final List rowList

    RegionRowImpl(final List rowList) {
        this.rowList = rowList
    }

    Long getId() { rowList[0] as Long }

    String getName() { rowList[1] as String }

    String getCytoband() { rowList[2] as String }

    Platform getPlatform() {
        throw new UnsupportedOperationException('Getter for get platform is not implemented')
    }

    String getChromosome() { rowList[3] as String }

    Long getStart() { rowList[4] as Long }

    Long getEnd() { rowList[5] as Long }

    Integer getNumberOfProbes() { rowList[6] as Integer }

    @Override
    String getBioMarker() { rowList[7] as String }

    @Override
    String getLabel() {
        cytoband
    }

    @Override
    public java.lang.String toString() {
        return "RegionImpl{" +
                "rowList=" + rowList +
                '}';
    }
}
