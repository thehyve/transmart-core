package org.transmartproject.webservices

import grails.converters.JSON
import java.util.Date
import org.transmartproject.webservices.Study
import org.transmartproject.webservices.StudyJsonMarshaller

class TransmartMarshallerRegistrar {

	@javax.annotation.PostConstruct
    void registerMarshallers() {
    	// example marshaller
        // JSON.registerObjectMarshaller(Study) { Study study ->
        //     log.error "In marshaller for Study:$study"
        //     return "yep"
        // }
        JSON.registerObjectMarshaller(new StudyJsonMarshaller())

        log.error "IN REGISTER MARSHALLERS!!!!!!!!!!!!!!!!!!!!!!!!!!"
        JSON.registerObjectMarshaller(Date) { Date date ->
            log.error "In marshaller for date:$date"
            return date?.toString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        }

    }
}