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

package org.transmartproject.db.dataquery.highdim.protein

import groovy.transform.EqualsAndHashCode
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.i2b2data.PatientDimension

@EqualsAndHashCode(includes = 'assay,annotation')
class DeSubjectProteinData implements Serializable {

    BigDecimal intensity
    BigDecimal zscore
    BigDecimal logIntensity

    // irrelevant...
    //String     trialName
    //String     component
    //String     timepoint
    //BigDecimal patientId
    //String     subjectId
    //String     geneSymbol
    //Long       geneId
    //BigDecimal NValue
    //BigDecimal meanIntensity
    //BigDecimal stddevIntensity
    //BigDecimal medianIntensity
    //BigDecimal logIntensity

    DeProteinAnnotation jAnnotation

    static belongsTo = [
            assay:      DeSubjectSampleMapping,
            annotation: DeProteinAnnotation,
            patient:    PatientDimension
    ]

    static mapping = {
        table schema:    'deapp'
        id    composite: ['assay', 'annotation']

        assay      column: 'assay_id'
        annotation column: 'protein_annotation_id'
        patient    column: 'patient_id'

        // this is needed due to a Criteria bug.
        // see https://forum.hibernate.org/viewtopic.php?f=1&t=1012372
        jAnnotation column: 'protein_annotation_id', updateable: false, insertable: false
        version false
    }

    static constraints = {
        intensity    nullable: true, scale: 4
        zscore       nullable: true, scale: 4
        logIntensity nullable: true, scale: 4

        // irrelevant:
        //trialName       nullable: true, maxSize: 15
        //component       nullable: true, maxSize: 200
        //patientId       nullable: true
        //subjectId       nullable: true, maxSize: 10
        //geneSymbol      nullable: true, maxSize: 100
        //geneId          nullable: true
        //timepoint       nullable: true, maxSize: 20
        //'NValue'        nullable: true
        //meanIntensity   nullable: true
        //stddevIntensity nullable: true
        //medianIntensity nullable: true
    }
}
