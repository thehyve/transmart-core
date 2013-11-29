package org.transmartproject.db.dataquery.highdim.chromoregion

import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.chromoregion.Region
import org.transmartproject.db.dataquery.highdim.AbstractDataRow

final class RegionRow extends AbstractDataRow implements Region {

    final List rowList

    RegionRow(final List rowList) {
        this.rowList = rowList
    }

    Long getId() { rowList[8] as Long }

    String getCytoband() { rowList[9] as String }

    Platform getPlatform() {
        throw new UnsupportedOperationException('Getter for get platform is not implemented')
    }

    String getChromosome() { rowList[10] as String }

    Long getStart() { rowList[11] as Long }

    Long getEnd() { rowList[12] as Long }

    Integer getNumberOfProbes() { rowList[13] as Integer }

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
