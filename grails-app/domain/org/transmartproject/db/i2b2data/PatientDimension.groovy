package org.transmartproject.db.i2b2data

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.db.highdim.DeSubjectSampleMapping

class PatientDimension implements Patient {

    String sourcesystemCd

    /* unused; only id is used */
    String vitalStatusCd
    Date   birthDate
    Date   deathDate
    String sexCd
    Long   ageInYearsNum
    String languageCd
    String raceCd
    String maritalStatusCd
    String religionCd
    String zipCd
    String statecityzipPath
    String incomeCd
    String patientBlob
    Date   updateDate
    Date   downloadDate
    Date   importDate
    Long   uploadId

    static hasMany = [assays: DeSubjectSampleMapping]

	static mapping = {
        table       name: 'PATIENT_DIMENSION', schema: 'I2B2DEMODATA'

        id          generator: "assigned", column: 'PATIENT_NUM', type: Long
        patientBlob sqlType:   'text'

		version false
	}

	static constraints = {
        vitalStatusCd    nullable: true, maxSize: 50
        birthDate        nullable: true
        deathDate        nullable: true
        sexCd            nullable: true, maxSize: 50
        ageInYearsNum    nullable: true
        languageCd       nullable: true, maxSize: 50
        raceCd           nullable: true, maxSize: 50
        maritalStatusCd  nullable: true, maxSize: 50
        religionCd       nullable: true, maxSize: 50
        zipCd            nullable: true, maxSize: 10
        statecityzipPath nullable: true, maxSize: 700
        incomeCd         nullable: true, maxSize: 50
        patientBlob      nullable: true
        updateDate       nullable: true
        downloadDate     nullable: true
        importDate       nullable: true
        sourcesystemCd   nullable: true, maxSize: 50
        uploadId         nullable: true
	}

    @Override
    String getTrial() {
        sourcesystemCd?.split(/:/, 2)[0]
    }

    @Override
    String getInTrialId() {
        if (sourcesystemCd == null) {
            return null;
        }
        (sourcesystemCd.split(/:/, 2) as List)[1] /* cast to avoid exception */
    }
}
