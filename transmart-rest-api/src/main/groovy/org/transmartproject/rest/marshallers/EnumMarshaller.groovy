package org.transmartproject.rest.marshallers

import grails.converters.JSON
import groovy.transform.CompileStatic
import org.grails.web.converters.exceptions.ConverterException
import org.grails.web.converters.marshaller.ObjectMarshaller
import org.springframework.stereotype.Component
import org.transmartproject.db.multidimquery.query.Operator

@Component
@CompileStatic
class EnumMarshaller implements ObjectMarshaller<JSON> {

    @Override
    boolean supports(Object object) {
        return object != null && object instanceof Enum
    }

    @Override
    void marshalObject(Object object, JSON converter) throws ConverterException {
        if (object instanceof Operator) {
            Operator operator = (Operator)object
            converter.value operator.symbol
        } else {
            Enum value = (Enum) object
            converter.value value.name()
        }
    }
}
