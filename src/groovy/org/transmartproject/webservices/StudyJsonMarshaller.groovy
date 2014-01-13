package org.transmartproject.webservices

import grails.converters.JSON

class StudyJsonMarshaller {

  	void register() {
  		JSON.registerObjectMarshaller( Study ) { Study study ->
            log.error "MARSHALLING STUDY!!!!!"
            return [
                name: study.name
            ]
  		}
  	}
}