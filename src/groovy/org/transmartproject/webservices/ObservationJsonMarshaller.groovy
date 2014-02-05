package org.transmartproject.webservices

import grails.converters.JSON

class ObservationJsonMarshaller {

  	void register() {
  		JSON.registerObjectMarshaller( Observation ) { Observation observation ->
            return [
                name: observation.name,
                id: observation.id
            ]
  		}
  	}
}