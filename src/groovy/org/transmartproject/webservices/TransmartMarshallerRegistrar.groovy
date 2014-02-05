package org.transmartproject.webservices

import grails.converters.JSON
import java.util.Date
import org.transmartproject.webservices.StudyJsonMarshaller

class TransmartMarshallerRegistrar {

    List marshallers = []

    def register() {
        marshallers.each{ it.register() }
    }
}