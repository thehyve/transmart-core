/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.dataquery.highdim.chromoregion

import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.chromoregion.RegionRow
import org.transmartproject.db.dataquery.highdim.AbstractDataRow

final class RegionRowImpl extends AbstractDataRow implements RegionRow, BioMarkerDataRow {

    Long     id
    String   name
    String   cytoband
    String   chromosome
    Long     start
    Long     end
    Integer  numberOfProbes
    String   bioMarker
    Platform platform

    RegionRowImpl() {}  // Enforce that default constructor is generated along with next explicit constructor
                        // Else map constructors will fail.

    @Override
    String getLabel() {
        name
    }

    @Override
    public java.lang.String toString() {
        return "RegionRowImpl{" +
                "regionId=" + id +
                ", regionName=" + name +
                ", data=" + data.toListString() +
                ", assayIndexMap=" + assayIndexMap.toMapString() +
                '}';
    }
}
