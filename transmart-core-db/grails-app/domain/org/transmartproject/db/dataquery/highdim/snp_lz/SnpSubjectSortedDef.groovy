/*
 * Copyright © 2013-2016 The Hyve B.V.
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

package org.transmartproject.db.dataquery.highdim.snp_lz

import org.transmartproject.db.i2b2data.PatientDimension

class SnpSubjectSortedDef {

    String trialName
    Integer patientPosition
    String subjectId
    String assayId

    static belongsTo = [
            patient: PatientDimension,
            bioAssayPlatform: CoreBioAssayPlatform,
    ]

    static mapping = {
        table               schema:  'deapp',  name:  'de_snp_subject_sorted_def'
        id                  column:  'snp_subject_sorted_def_id',        generator:  'assigned'
        patient             column:  'patient_num'
        bioAssayPlatform    column:  'bio_assay_platform_id', generator:  'assigned'
        version             false
    }

    static constraints = {
        trialName           nullable:  true
        patientPosition     nullable:  true
        patient             nullable:  true
        bioAssayPlatform    nullable:  true
        subjectId           nullable:  true
        assayId             nullable:  true
    }
}
