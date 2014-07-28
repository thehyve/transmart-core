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

package org.transmartproject.db.dataquery.highdim.rbm

import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping

class DeSubjectRbmData {

    BigDecimal value
    BigDecimal logIntensity
    BigDecimal zscore
    String unit

    static hasMany = [annotations: DeRbmAnnotation]

    static belongsTo = [
            annotations: DeRbmAnnotation,
            assay: DeSubjectSampleMapping
    ]

    static mapping = {
        table schema: 'deapp', name: 'de_subject_rbm_data'
        id generator: 'sequence', params: [sequence: 'de_subject_rbm_data_seq', schema: 'deapp']
        assay column: 'assay_id'
        annotations joinTable: [
                name: 'de_rbm_data_annotation_join',
                column: 'annotation_id',
                key: 'data_id']
        version false
    }

    static constraints = {
        assay nullable: true
        value nullable: true, scale: 17
        zscore nullable: true, scale: 17
        logIntensity nullable: true, scale: 17
        unit nullable: true, maxSize: 150
    }
}
