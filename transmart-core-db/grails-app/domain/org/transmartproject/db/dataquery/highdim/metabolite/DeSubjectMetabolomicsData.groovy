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

import groovy.transform.EqualsAndHashCode
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping

@EqualsAndHashCode(includes = 'assay,annotation')
class DeSubjectMetabolomicsData implements Serializable {

    BigDecimal zscore
    BigDecimal rawIntensity
    BigDecimal logIntensity
    DeMetaboliteAnnotation jAnnotation

    static belongsTo = [
            assay:      DeSubjectSampleMapping,
            annotation: DeMetaboliteAnnotation,
    ]

    static mapping = {
        table      schema:    'deapp'
        id         composite: ['assay', 'annotation']

        assay      column:    'assay_id'
        annotation column:    'metabolite_annotation_id'

        // this is needed due to a Criteria bug.
        // see https://forum.hibernate.org/viewtopic.php?f=1&t=1012372
        jAnnotation column: 'metabolite_annotation_id', updateable: false, insertable: false
        version false
    }

    static constraints = {
        zscore scale: 5
        rawIntensity nullable: true, scale: 5
        logIntensity nullable: true, scale: 5
    }
}
