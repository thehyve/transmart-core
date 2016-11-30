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

import java.sql.Clob

class GenotypeProbeAnnotation {

    String snpName
    String chromosome
    Long pos
    String ref
    String alt
    String geneInfo
    String variationClass
    String strand
    String exonIntron
    String genomeBuild
    String snpSource
    BigDecimal recombinationRate
    BigDecimal recombinationMap
    String regulomeScore
    Clob refClob
    Clob altClob

    String createdBy
    String modifiedBy
    Date dateCreated
    Date lastUpdated

    static mapping = {
        table        schema: 'biomart'

        id           column: 'genotype_probe_annotation_id', generator: 'assigned'
        chromosome   column: 'chrom'
        dateCreated  column: 'created_date'
        lastUpdated  column: 'modified_date'

        version false
    }

    static constraints = {
        snpName            nullable: true,  maxSize: 50
        chromosome         nullable: true,  maxSize: 4
        pos                nullable: true
        ref                nullable: true,  maxSize: 4000
        alt                nullable: true,  maxSize: 4000
        geneInfo           nullable: true,  maxSize: 4000
        variationClass     nullable: true,  maxSize: 10
        strand             nullable: true,  maxSize: 1
        exonIntron         nullable: true,  maxSize: 10
        genomeBuild        nullable: true,  maxSize: 10
        snpSource          nullable: true,  maxSize: 10
        recombinationRate  nullable: true,  scale:   6
        recombinationMap   nullable: true,  scale:   6
        regulomeScore      nullable: true,  maxSize: 10
        refClob            nullable: true
        altClob            nullable: true

        createdBy          nullable: true,  maxSize: 30
        dateCreated        nullable: true
        modifiedBy         nullable: true,  maxSize: 30
        lastUpdated        nullable: true
    }
}
