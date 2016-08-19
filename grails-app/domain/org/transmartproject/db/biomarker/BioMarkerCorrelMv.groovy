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

package org.transmartproject.db.biomarker

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = [ 'bioMarker', 'associatedBioMarker' ] )
class BioMarkerCorrelMv implements Serializable {

    /* view in PostgreSQL, table in Oracle (sanofi) */

	String correlationType
    Long   mvId

    static belongsTo = [
            bioMarker:           BioMarkerCoreDb, /* maybe these can come from other tables */
            associatedBioMarker: BioMarkerCoreDb,
    ]

	static mapping = {
        table               schema:    'biomart'

        id                  composite: [ 'bioMarker', 'associatedBioMarker' ]

        bioMarker           column:    'bio_marker_id'
        associatedBioMarker column:    'asso_bio_marker_id'
        correlationType     column:    'correl_type'

		version false
	}

	static constraints = {
        bioMarker           nullable: false /* actually true in db */
        associatedBioMarker nullable: false /* idem */
        correlationType     nullable: true, maxSize: 15
        mvId                nullable: true
	}
}
