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

@Deprecated
class DeRcSnpInfo {

    String chromosome
    Long   pos
    String hgVersion
    String geneName
    String entrezId

    static mapping = {
        table      schema: 'deapp'
        id         column: 'snp_info_id', generator: 'assigned'
        chromosome column: 'chrom'
    }

    static constraints = {
        chromosome nullable: true, maxSize: 4
        hgVersion  nullable: true, maxSize: 10
        geneName   nullable: true, maxSize: 50
        entrezId   nullable: true, maxSize: 50
    }
}
