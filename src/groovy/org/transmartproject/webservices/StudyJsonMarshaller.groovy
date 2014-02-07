package org.transmartproject.webservices

import grails.converters.JSON
import org.transmartproject.db.ontology.I2b2

class StudyJsonMarshaller {

  	void register() {
  		JSON.registerObjectMarshaller( I2b2 ) { I2b2 study ->
            return [
                id: study.name,  // TODO using name as an id is INCORRECT and incomplete and just plain stupid
                name: study.name,
                key: study.key,
                fullName: study.fullName,
                dimensionCode: study.dimensionCode
            ]
  		}
  	}
}