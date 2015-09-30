/*
 * Copyright © 2013-2015 The Hyve B.V.
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

package org.transmartproject.db.dataquery.highdim.snp_lz

import org.transmartproject.core.dataquery.highdim.projections.AllDataProjection
import org.transmartproject.db.dataquery.highdim.projections.AllDataProjectionImpl

/**
 * Implements {@link AllDataProjection} for the snp_lz module.
 * {@link AllDataProjectionImpl} is not appropriate because it tries to add
 * projections to the Hibernate query based on the value of dataProperties.
 */
class SnpLzAllDataProjection implements AllDataProjection {

    final Map<String, Class> dataProperties = SnpLzCell
            .metaClass
            .properties
            .findAll { it.name != 'class' }
            .collectEntries { [it.name, it.type] }

    final Map<String, Class> rowProperties =
            ['snpName', 'a1', 'a2', 'imputeQuality', 'GTProbabilityThreshold',
             'minorAlleleFrequency', 'minorAllele', 'a1a1Count', 'a1a2Count',
             'a2a2Count', 'noCallCount'].collectEntries {
                def p = SnpLzCell.metaClass.getProperty(it)
                [p.name, p.type]
            }

    @Override
    Map<String, Object> doWithResult(Object object) {
        assert object instanceof Map
        object
    }
}
