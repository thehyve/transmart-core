package org.transmartproject.webservices

import grails.converters.JSON
import org.transmartproject.db.i2b2data.PatientDimension

class SubjectJsonMarshaller {

  	void register() {
  		JSON.registerObjectMarshaller( PatientDimension ) { PatientDimension subject ->
            return [
                id: subject.id,
                age: subject.age,
                birthDate: subject.birthDate,
				deathDate: subject.deathDate,
				downloadDate: subject.downloadDate,
				importDate: subject.importDate,
				incomeCd: subject.incomeCd,
				languageCd: subject.languageCd,
				maritalStatus: subject.maritalStatus,
				patientBlob: subject.patientBlob,
				race: subject.race,
				religion: subject.religion,
				sexCd: subject.sexCd,
				sourcesystemCd: subject.sourcesystemCd,
				statecityzipPath: subject.statecityzipPath,
				updateDate: subject.updateDate,
				uploadId: subject.uploadId,
				vitalStatusCd: subject.vitalStatusCd,
				zipCd: subject.zipCd
            ]
  		}
  	}
}