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

class BioDataCorrelationCoreDb {


    /* unidirectional relationship between
      the left bio marker and the right bio marker */
    static belongsTo = [ description:    BioDataCorrelDescr,
                         leftBioMarker:  BioMarkerCoreDb,
                         rightBioMarker: BioMarkerCoreDb ]

    static mapping = {
        table          name:   'bio_data_correlation',     schema:    'biomart'

        id             column: 'bio_data_correl_id',       generator: 'assigned'
        description    column: 'bio_data_correl_descr_id', cascade:   'save-update'
        leftBioMarker  column: 'bio_data_id'
        rightBioMarker column: 'asso_bio_data_id'

        version        false
    }
}
