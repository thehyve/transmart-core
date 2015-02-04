package org.transmartproject.db.dataquery.highdim.tworegion

import org.apache.commons.lang.NullArgumentException
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.db.dataquery.highdim.AbstractDataRow

/**
 * Created by j.hudecek on 23-1-2015.
 */
class JunctionsRow extends AbstractDataRow {

    @Override
    String getLabel() {
        def nonnull = data.findAll()
        if (nonnull.empty) {
            return ""
        }
        return nonnull*.label.join('; ')
    }

    public JunctionsRow(Map<AssayColumn, Integer> map, DeTwoRegionJunction r) {
        if (map == null) {
            throw new NullArgumentException("assayIndexMap can't be null")
        }
        assayIndexMap = map
        data = new ArrayList(map.size())
        data[assayIndexMap.find({it.key.id == r.assay.id}).value] = r.toMap()
        junction = r
    }

    DeTwoRegionJunction junction
}
