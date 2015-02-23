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

import org.transmartproject.core.biomarker.BioMarker

class BioMarkerCoreDb implements BioMarker {

    String name
    String description
    String organism
    String primarySourceCode
    String primaryExternalId
    String type

    static mapping = {
        table       name:   'bio_marker',    schema:    'biomart'
        id          column: 'bio_marker_id', generator: 'assigned'

        name        column: 'bio_marker_name'
        description column: 'bio_marker_description'
        type        column: 'bio_marker_type'

        version false
    }

    static constraints = {
        name              nullable: true, maxSize: 400
        description       nullable: true, maxSize: 2000
        organism          nullable: true, maxSize: 400
        primarySourceCode nullable: true, maxSize: 400
        primaryExternalId nullable: true, maxSize: 400
        type              maxSize:  400
    }
}
