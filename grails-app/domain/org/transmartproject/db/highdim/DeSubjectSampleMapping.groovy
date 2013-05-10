package org.transmartproject.db.highdim

import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.assay.SampleType
import org.transmartproject.core.dataquery.assay.Timepoint
import org.transmartproject.core.dataquery.assay.TissueType
import org.transmartproject.db.i2b2data.PatientDimension

class DeSubjectSampleMapping implements Assay {

    DeGplInfo platform

    String   siteId
    String   subjectId
    String   subjectType
    String   conceptCode

    String   trialName

    String   timepointName
    String   timepointCd

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
    String   sampleCd
    String   categoryCd
    String   sourceCd
    String   omicSourceStudy
    Long     omicPatientId


    static transients = ['timepoint', 'sampleType', 'tissueType']

    static belongsTo = [patient: PatientDimension]

    static mapping = {
        table          schema: 'deapp'

        id             column: "assay_id",    generator: "assigned"

        patient        column: 'patient_id', cascade: 'save-update'
        platform       column: 'gpl_id',     cascade: 'save-update'
        platformType   column: 'platform'
        platformTypeCd column: 'platform_cd'
        timepointName  column: 'timepoint'
        sampleTypeName column: 'sample_type'
        tissueTypeName column: 'tissue_type'

        sort           id:     'asc'

		version false
	}

	static constraints = {
        assayUid        nullable: true, maxSize: 100
        categoryCd      nullable: true, maxSize: 1000
        conceptCode     nullable: true, maxSize: 1000
        dataUid         nullable: true, maxSize: 100
        omicPatientId   nullable: true
        omicSourceStudy nullable: true, maxSize: 200
        patient         nullable: true
        patientUid      nullable: true, maxSize: 50
        platform        nullable: true
        platformType    nullable: true, maxSize: 50
        platformTypeCd  nullable: true, maxSize: 50
        rbmPanel        nullable: true, maxSize: 50
        sampleCd        nullable: true, maxSize: 200
        sampleId        nullable: true
        sampleTypeCd    nullable: true, maxSize: 50
        sampleTypeName  nullable: true, maxSize: 100
        siteId          nullable: true, maxSize: 100
        sourceCd        nullable: true, maxSize: 50
        subjectId       nullable: true, maxSize: 100
        subjectType     nullable: true, maxSize: 100
        timepointCd     nullable: true, maxSize: 50
        timepointName   nullable: true, maxSize: 100
        tissueTypeCd    nullable: true, maxSize: 50
        tissueTypeName  nullable: true, maxSize: 100
        trialName       nullable: true, maxSize: 30
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
