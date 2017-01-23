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

package org.transmartproject.db.dataquery.highdim

import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.assay.SampleType
import org.transmartproject.core.dataquery.assay.Timepoint
import org.transmartproject.core.dataquery.assay.TissueType
import org.transmartproject.db.i2b2data.PatientDimension

class DeSubjectSampleMapping implements Assay {

    DeGplInfo platform

    String   siteId
    String   patientInTrialId
    String   subjectType
    String   conceptCode

    String   trialName

    String   timepointName
    String   timepointCd

    String   sampleCode
    String   sampleTypeName
    String   sampleTypeCd

    String   tissueTypeName
    String   tissueTypeCd

    /* unused */
    String   patientUid
    String   assayUid
    String   platformType
    String   platformTypeCd
    String   dataUid
    String   rbmPanel
    Long     sampleId
    String   categoryCd
    String   sourceCd
    String   omicSourceStudy
    Long     omicPatientId


    static transients = ['timepoint', 'sampleType', 'tissueType']

    static belongsTo = [patient: PatientDimension]

    static mapping = {
        table            schema: 'deapp'

        id               column: 'assay_id',    generator: 'assigned'

        patient          column: 'patient_id', cascade: 'save-update'
        patientInTrialId column: 'subject_id'
        platform         column: 'gpl_id',     cascade: 'save-update'
        platformType     column: 'platform'
        platformTypeCd   column: 'platform_cd'
        timepointName    column: 'timepoint'
        sampleCode       column: 'sample_cd'
        sampleTypeName   column: 'sample_type'
        tissueTypeName   column: 'tissue_type'

        sort           id:     'asc'

		version false
	}

	static constraints = {
        assayUid         nullable: true, maxSize: 100
        categoryCd       nullable: true, maxSize: 1000
        conceptCode      nullable: true, maxSize: 1000
        dataUid          nullable: true, maxSize: 100
        omicPatientId    nullable: true
        omicSourceStudy  nullable: true, maxSize: 200
        patient          nullable: true
        patientUid       nullable: true, maxSize: 50
        platform         nullable: true
        platformType     nullable: true, maxSize: 50
        platformTypeCd   nullable: true, maxSize: 50
        rbmPanel         nullable: true, maxSize: 50
        sampleCode       nullable: true, maxSize: 200
        sampleId         nullable: true
        sampleTypeCd     nullable: true, maxSize: 50
        sampleTypeName   nullable: true, maxSize: 100
        siteId           nullable: true, maxSize: 100
        sourceCd         nullable: true, maxSize: 50
        patientInTrialId nullable: true, maxSize: 100
        subjectType      nullable: true, maxSize: 100
        timepointCd      nullable: true, maxSize: 50
        timepointName    nullable: true, maxSize: 100
        tissueTypeCd     nullable: true, maxSize: 50
        tissueTypeName   nullable: true, maxSize: 100
        trialName        nullable: true, maxSize: 30
	}

    //  region Properties with values generated on demand
    @Override
    Timepoint getTimepoint() {
        new Timepoint(code: timepointCd, label: timepointName)
    }

    @Override
    SampleType getSampleType() {
        new SampleType(code: sampleTypeCd, label: sampleTypeName)
    }

    @Override
    TissueType getTissueType() {
        new TissueType(code: tissueTypeCd, label: tissueTypeName)
    }
//  endregion
}
