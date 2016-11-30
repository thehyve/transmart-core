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

package org.transmartproject.db.dataquery.highdim.chromoregion

import org.transmartproject.core.dataquery.highdim.chromoregion.Region
import org.transmartproject.db.dataquery.highdim.DeGplInfo

class DeChromosomalRegion implements Region {

    String  chromosome
    Long    start
    Long    end
    Integer numberOfProbes
    String  name
    String  cytoband
    String  geneSymbol
    Long    geneId

    /* unused */
    String  organism

    static belongsTo = [platform: DeGplInfo]

	static mapping = {
        table          schema: 'deapp'

        id             column:  "region_id",  generator: "assigned"

        start          column: 'start_bp'
        end            column: 'end_bp'
        name           column: 'region_name'
        numberOfProbes column: 'num_probes'
        platform       column: 'gpl_id'

        version false

	}

	static constraints = {
        platform       nullable: true
        chromosome     nullable: true, maxSize: 2
        start          nullable: true
        end            nullable: true
        numberOfProbes nullable: true
        name           nullable: true, maxSize: 100
        cytoband       nullable: true, maxSize: 100
        geneSymbol     nullable: true, maxSize: 100
        geneId         nullable: true
        organism       nullable: true, maxSize: 200
	}

}
