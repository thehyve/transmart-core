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

package org.transmartproject.db.bioassay

import org.transmartproject.db.biomarker.BioMarkerCoreDb

class BioAssayFeatureGroupCoreDb implements Serializable {
    String name
    String type

    static hasMany = [ markers: BioMarkerCoreDb ]

    static mapping = {
        table schema: 'biomart', name: 'bio_assay_feature_group'

        id column: 'bio_assay_feature_group_id', generator:'assigned'

        name column: 'feature_group_name'
        type column: 'feature_group_type'

        markers joinTable: [ name: 'bio_assay_data_annotation', key: 'bio_assay_feature_group_id' ]

        version false
    }

    static constraints = {
        name maxSize: 100
        type maxSize: 50
    }
}
