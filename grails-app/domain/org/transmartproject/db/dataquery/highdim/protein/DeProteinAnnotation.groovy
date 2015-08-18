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

import org.transmartproject.db.dataquery.highdim.DeGplInfo

class DeProteinAnnotation {

    String   peptide
    String   uniprotId
    String   uniprotName

    String   chromosome
    Long     startBp
    Long     endBp

    // irrelevant
    //String biomarkerId
    //String organism

    static belongsTo = [ platform: DeGplInfo ]

    static hasMany = [ dataRows: DeSubjectProteinData ]

    static mappedBy = [ dataRows: 'annotation' ]

    static mapping = {
        table    schema:    'deapp'
        id       generator: 'assigned'
        platform column:    'gpl_id'

        version   false
    }

    static constraints = {
        peptide     maxSize:  800
        uniprotId   nullable: true, maxSize: 200
        uniprotName nullable: true, maxSize: 200
        chromosome  nullable: true
        startBp     nullable: true
        endBp       nullable: true

        //biomarkerId nullable: true, maxSize: 400
        //organism    nullable: true, maxSize: 800
    }
}
