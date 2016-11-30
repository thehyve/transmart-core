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
import org.hibernate.criterion.ProjectionList
import org.hibernate.criterion.Projections
import org.transmartproject.core.dataquery.highdim.projections.AllDataProjection
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection

/**
 * Implements {@link AllDataProjection} for the snp_lz module.
 * {@link AllDataProjectionImpl} is not appropriate because it tries to add
 * projections to the Hibernate query based on the value of dataProperties.
 */
class SnpLzAllDataProjection implements
        AllDataProjection, CriteriaProjection<Map<String, Object>> {

    private static final ArrayList<String> DOMAIN_CLASS_PROPERTIES_FETCHED =
            ['gpsByProbeBlob', 'gtsByProbeBlob', 'doseByProbeBlob']

    final Map<String, Class> dataProperties = SnpLzAllDataCell
            .metaClass
            .properties
            .findAll { !(it.name in ['class', 'likelyAllele1', 'likelyAllele2', 'empty']) }
            .collectEntries { [it.name, it.type] }

    final Map<String, Class> rowProperties =
            ['snpName', 'a1', 'a2', 'imputeQuality', 'GTProbabilityThreshold',
             'minorAlleleFrequency', 'minorAllele', 'a1a1Count', 'a1a2Count',
             'a2a2Count', 'noCallCount'].collectEntries {
                def p = SnpLzRow.metaClass.properties.find { n -> n.name == it }
            }

    @Override
    ImmutableMap<String, Class> getDataProperties() { _dataProperties }
    private static final ImmutableMap<String, Class> _dataProperties = ImmutableMap.copyOf(
            // Ensure a logical order
            ['probabilityA1A1', 'probabilityA1A2', 'probabilityA2A2', 'likelyGenotype', 'minorAlleleDose']
                    .collectEntries {
                def p = SnpLzAllDataCell.metaClass.properties.find { n -> n.name == it }
                [p.name, p.type]
            })

    @Override
    ImmutableMap<String, Class> getRowProperties() { _rowProperties }
    private static final ImmutableMap<String, Class> _rowProperties = ImmutableMap.copyOf(
        ['snpName', 'chromosome', 'position', 'a1', 'a2', 'imputeQuality', 'GTProbabilityThreshold',
         'minorAlleleFrequency', 'minorAllele', 'a1a1Count', 'a1a2Count', 'a2a2Count', 'noCallCount'].collectEntries {
            def p = SnpLzRow.metaClass.properties.find { n -> n.name == it }
            [p.name, p.type]
        })

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder builder) {
        def projection = builder.instance.projection

        assert projection instanceof ProjectionList

        DOMAIN_CLASS_PROPERTIES_FETCHED.each { field ->
            assert SnpDataByProbeCoreDb.metaClass
                    .properties.find { it.name == field }

            // add an alias to make this ALIAS_TO_ENTITY_MAP-friendly
            projection.add(
                    Projections.alias(
                            Projections.property(field),
                            field))
        }
    }

    @Override
    Map<String, Object> doWithResult(Object object) {
        assert object instanceof Map
        object
    }
}
