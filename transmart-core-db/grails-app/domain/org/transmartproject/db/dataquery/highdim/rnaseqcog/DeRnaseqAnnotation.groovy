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

package org.transmartproject.db.dataquery.highdim.rnaseqcog

import org.transmartproject.db.dataquery.highdim.DeGplInfo

class DeRnaseqAnnotation implements Serializable {

    String transcriptId
    String geneSymbol
    String geneId /* the Entrez accession; "primary external id" */

    // irrelevant
    //String organism

    static transients = ['id']

    static belongsTo = [ platform: DeGplInfo ]

    static mapping = {
        table    schema: 'deapp'
        id       name: 'transcriptId', generator: 'assigned'

        platform column: 'gpl_id'
        version  false
    }

    static constraints = {
        transcriptId maxSize: 50
        geneSymbol   nullable: true, maxSize: 50
        geneId       nullable: true, maxSize: 50

        //organism nullable: true, maxSize: 30
    }

    void setId(String id) {
        transcriptId = id
    }

    String getId() {
        transcriptId
    }
}
