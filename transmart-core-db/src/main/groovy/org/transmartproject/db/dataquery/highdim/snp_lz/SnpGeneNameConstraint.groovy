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

import java.util.List;

import grails.orm.HibernateCriteriaBuilder

import org.transmartproject.db.dataquery.highdim.snp_lz.DeSnpGeneMap
import org.transmartproject.db.dataquery.highdim.dataconstraints.CriteriaDataConstraint

/**
 * A data constraint that limits the returned SNPs to those that belong to
 * the selected gene names.
 * The SNP names are fetched from the {@link DeSnpGeneMap} table and added as
 * constraints on the <var>property</var> field.
 *
 * Ideally, the constraint would be implemented as a subquery, instead of
 * enumerating the SNP names. However, that is not yet supported in Grails 2.3.7.
 *
 * To prevent Oracle errors about the maximum number of elements in an 'in'
 * constraint, the constraint is split into multiple disjunctive constraints.
 *
 * @author gijs@thehyve.nl
 */
class SnpGeneNameConstraint implements CriteriaDataConstraint {

    String property

    List<String> geneNames = []

    /**
     * Get the SNP names for a list of gene names from {@link DeSnpGeneMap}.
     *
     * @return the list of SNP names.
     */
    List<String> getSnpNamesForGeneNames(List<String> geneNames) {
        SortedSet<String> snpNames = [] as SortedSet
        for (String geneName: geneNames) {
            List<String> names = DeSnpGeneMap.createCriteria()
            .list {
                eq 'geneName', geneName
                order 'snpName', 'asc'
                projections {
                  distinct('snpName')
                }
            }
            snpNames.addAll(names)
        }
        snpNames.toList()
    }

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder criteria) {
        List<String> snpNames = getSnpNamesForGeneNames(geneNames)
        // split the list of SNP names into chuncks of max. 500.
        List<List<String>> chunks = snpNames.collate(500)
        log.debug "Number of SNP names: ${snpNames.size()}"
        log.debug "Number of chunks: ${chunks.size()}"
        criteria.with {
            or {
                chunks.collect { names ->
                    'in' property, names
                }
            }
        }
    }
}
