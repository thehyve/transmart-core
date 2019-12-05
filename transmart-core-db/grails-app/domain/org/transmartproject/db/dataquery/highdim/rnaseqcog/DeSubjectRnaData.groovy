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

package org.transmartproject.db.dataquery.highdim.rnaseqcog

import groovy.transform.EqualsAndHashCode
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping

@EqualsAndHashCode(includes = [ 'assay', 'annotation' ])
class DeSubjectRnaData implements Serializable {

    BigDecimal rawIntensity
    BigDecimal logIntensity
    BigDecimal zscore

    // irrelevant
    //String     trialSource
    //String     trialName
    //Long       patientId

    static belongsTo = [
            annotation: DeRnaseqAnnotation,
            assay:      DeSubjectSampleMapping
    ]

    static mapping = {
        table schema: 'deapp'

        id composite: [ 'assay', 'annotation' ]

        annotation  column: 'probeset_id' // poor name; no probes involved
        assay       column: 'assay_id'

        version     false

    }

    static constraints = {
        annotation   nullable: true
        assay        nullable: true
        rawIntensity nullable: true, scale: 4
        logIntensity nullable: true, scale: 4
        zscore       nullable: true, scale: 4

        // irrelevant
        //trialSource  nullable: true, maxSize: 200
        //trialName    nullable: true, maxSize: 50
        //patientId    nullable: true
    }
}
