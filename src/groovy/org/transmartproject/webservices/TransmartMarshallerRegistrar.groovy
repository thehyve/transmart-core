package org.transmartproject.webservices

class TransmartMarshallerRegistrar {

    List marshallers

    def register() {
        marshallers.each{ it.register() }
    }
}
