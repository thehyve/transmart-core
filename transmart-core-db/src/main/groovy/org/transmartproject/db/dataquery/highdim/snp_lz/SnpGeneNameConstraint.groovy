/*
 * Copyright © 2016 The Hyve B.V.
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

import groovy.util.logging.Slf4j
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Order
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Subqueries

import grails.orm.HibernateCriteriaBuilder

import org.transmartproject.db.dataquery.highdim.dataconstraints.CriteriaDataConstraint

/**
 * A data constraint that limits the returned SNPs to those that belong to
 * the selected gene names.
 * The SNP names are selected using subqueries on the {@link DeSnpGeneMap} table.
 * A constraint is added that the <var>property</var> field should be in the selected
 * set of SNP names.
 *
 * @author gijs@thehyve.nl
 */
@Slf4j
class SnpGeneNameConstraint implements CriteriaDataConstraint {

    String property

    List<String> geneNames = []

    /**
     * Add a constraint to select SNPs that are associated with any of the gene names
     * in {@link #geneNames} with a link in {@link DeSnpGeneMap}.
     */
    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder criteria) {
        log.debug "Number of gene names: ${geneNames.size()}"
        criteria.add(Subqueries.propertyIn(property,
                DetachedCriteria.forClass(DeSnpGeneMap)
                        .setProjection(Projections.distinct(Projections.property('snpName')))
                        .add(Restrictions.in('geneName', geneNames))))
    }
}
