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

package org.transmartproject.db.dataquery.highdim.mrna

import groovy.transform.EqualsAndHashCode
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.i2b2data.PatientDimension

@EqualsAndHashCode(includes = [ 'assay', 'probe' ])
class DeSubjectMicroarrayDataCoreDb implements Serializable {

    String     trialName
    BigDecimal rawIntensity
    BigDecimal logIntensity /* log2(rawIntensity) */
    BigDecimal zscore

    /* not mapped (only used in Oracle?) */
    //String     trialSource

    /* not mapped (not used in practice) */
    //Long       sampleId
    //String     subjectId
    //BigDecimal newRaw
    //BigDecimal newLog
    //BigDecimal newZscore

    static belongsTo = [
            probe: DeMrnaAnnotationCoreDb,
            assay: DeSubjectSampleMapping,
            patient: PatientDimension,
    ]

    static mapping = {
        table    schema: 'deapp', name: 'de_subject_microarray_data'

        id       composite: [ 'assay', 'probe' ]

        probe    column: 'probeset_id'
        assay    column: 'assay_id'
        patient  column: 'patient_id'

        version  false
    }

    static constraints = {
        trialName    nullable: true, maxSize: 50
        probe        nullable: true
        assay        nullable: true
        patient      nullable: true
        rawIntensity nullable: true
        logIntensity nullable: true, scale: 4
        zscore       nullable: true
        //trialSource  nullable: true, maxSize: 200
        //sampleId     nullable: true
        //subjectId    nullable: true, maxSize: 50
        //newRaw       nullable: true, scale:   4
        //newLog       nullable: true, scale:   4
        //newZscore    nullable: true, scale:   4
    }
}
