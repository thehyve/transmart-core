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

package org.transmartproject.db.dataquery.highdim.protein

import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.chromoregion.RegionRow
import org.transmartproject.db.dataquery.highdim.AbstractDataRow

class ProteinDataRow extends AbstractDataRow implements BioMarkerDataRow<Object>, RegionRow {

    Long     id
    String   uniprotName
    Platform platform
    String   chromosome
    Long     start
    Long     end

    String peptide

    @Override
    String getLabel() {
        peptide
    }

    @Override
    String getBioMarker() {
        uniprotName
    }

    @Override
    public String toString() {
        com.google.common.base.Objects.toStringHelper(this)
                .add('data', data).toString()
    }

    @Override
    String getName() {
        peptide
    }

    @Override
    String getCytoband() {
        return null
    }

    @Override
    Integer getNumberOfProbes() {
        return null
    }
}
