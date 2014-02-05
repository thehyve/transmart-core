package org.transmartproject.webservices

import grails.converters.JSON
import org.transmartproject.db.ontology.I2b2

class StudyJsonMarshaller {

  	void register() {
  		JSON.registerObjectMarshaller( I2b2 ) { I2b2 study ->
            return [
                name: study.name,
                key: study.key,
                fullName: study.fullName,
                dimensionCode: study.dimensionCode
            ]
  		}
  	}
}