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

package org.transmartproject.db.dataquery.highdim.mirna

import org.transmartproject.db.dataquery.highdim.DeGplInfo

class DeQpcrMirnaAnnotation implements Serializable {

    String mirnaId
    String detector
    String gplId

    static belongsTo = [ platform: DeGplInfo ]
    static hasMany = [dataRows: DeSubjectMirnaData]
    static mappedBy = [dataRows: 'probe']

    // unused or irrelevant:
    //String idRef
    //String probeId

    //String organism

    static mapping = {
        table    schema: 'deapp'
        id       column: 'probeset_id', generator: 'assigned'
        platform column: 'gpl_id'
        detector column: 'mirna_symbol' // column name is scheduled to be changed
        gplId    insertable: false, updateable: false
        version  false
    }

    static constraints = {
        mirnaId  nullable: true, maxSize: 100
        detector nullable: true, maxSize: 100
        platform nullable: true
        // unused or irrelevant:
        //idRef       nullable: true, maxSize: 100
        //probeId     nullable: true, maxSize: 100
        //organism    nullable: true, maxSize: 2000
    }
}
