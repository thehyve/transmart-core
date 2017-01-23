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

package org.transmartproject.db.dataquery.highdim.mrna

import org.transmartproject.db.biomarker.BioMarkerCoreDb

class DeMrnaAnnotationCoreDb {

    String     gplId
    String     probeId    /* a string user probe name */
    String     geneSymbol
    Long       geneId     /* aka primary external id */
    String     organism

    static transients = [ 'bioMarkerGene' ]

    static mapping = {
        id      column: 'probeset_id',       generator: 'assigned'
        table   name:  'de_mrna_annotation', schema:     'deapp'

        sort    id: 'asc'

        version false
    }

    static constraints = {
        gplId      nullable: true, maxSize: 100
        probeId    nullable: true, maxSize: 100
        geneSymbol nullable: true, maxSize: 100
        geneId     nullable: true
        organism   nullable: true, maxSize: 200
    }

    BioMarkerCoreDb getBioMarkerGene() {
        BioMarkerCoreDb.findByExternalId(geneId as String)
    }
}
