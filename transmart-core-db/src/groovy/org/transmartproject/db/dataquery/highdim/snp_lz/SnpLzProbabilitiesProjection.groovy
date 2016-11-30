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

import com.google.common.collect.ImmutableMap
import grails.orm.HibernateCriteriaBuilder
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.hibernate.criterion.ProjectionList
import org.hibernate.criterion.Projections
import org.transmartproject.core.dataquery.highdim.projections.MultiValueProjection
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection

/**
 * Returns only the probabilities
 * *
 */
@CompileStatic
class SnpLzProbabilitiesProjection implements MultiValueProjection, CriteriaProjection<SnpLzProbabilitiesCell> {

    private static final String DOMAIN_CLASS_PROPERTY = 'gpsByProbeBlob'

    final Map<String, Class> dataProperties = ['probabilityA1A1', 'probabilityA1A2', 'probabilityA2A2']
            .collectEntries { [it, Double] }

    @CompileStatic(TypeCheckingMode.SKIP)
    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder builder) {
        def projection = builder.instance.projection
        assert projection instanceof ProjectionList

        projection.add(
                Projections.alias(
                        Projections.property(DOMAIN_CLASS_PROPERTY),
                        DOMAIN_CLASS_PROPERTY))
    }

    @Override
    SnpLzProbabilitiesCell doWithResult(Object o) {
        assert o instanceof SnpLzAllDataCell
        SnpLzAllDataCell cell = (SnpLzAllDataCell) o
        return new SnpLzProbabilitiesCell(
                cell.probabilityA1A1,
                cell.probabilityA1A2,
                cell.probabilityA2A2)
    }
}

@CompileStatic
@Canonical
class SnpLzProbabilitiesCell {double probabilityA1A1, probabilityA1A2, probabilityA2A2}
