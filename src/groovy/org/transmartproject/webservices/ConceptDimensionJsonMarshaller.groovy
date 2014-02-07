package org.transmartproject.webservices

import grails.converters.JSON
import org.transmartproject.db.i2b2data.ConceptDimension

class ConceptDimensionJsonMarshaller {

  	void register() {
  		JSON.registerObjectMarshaller( ConceptDimension ) { ConceptDimension conceptDimension ->
            return [
                path: conceptDimension.conceptPath,
                id: conceptDimension.conceptCode,
            ]
  		}
  	}
}