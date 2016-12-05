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

package org.transmartproject.db.dataquery.highdim.metabolite

import org.transmartproject.db.dataquery.highdim.DeGplInfo

class DeMetaboliteSubPathway {

    DeGplInfo gplId
    String    name

    static hasMany = [annotations: DeMetaboliteAnnotation]

    static belongsTo = [superPathway: DeMetaboliteSuperPathway]

    static mapping = {
        table        schema:    'deapp',   name: 'de_metabolite_sub_pathways'
        id           generator: 'assigned'

        annotations  joinTable: [name:   'de_metabolite_sub_pway_metab',
                                 key:    'sub_pathway_id',
                                 column: 'metabolite_id']
        name         column: 'sub_pathway_name'
        superPathway column: 'super_pathway_id'

        version     false
    }

    static constraints = {
        name           maxSize:  200
        superPathway   nullable: true
    }
}
