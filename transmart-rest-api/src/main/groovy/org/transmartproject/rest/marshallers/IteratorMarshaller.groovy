package org.transmartproject.rest.marshallers

import grails.converters.JSON
import org.grails.web.converters.marshaller.ObjectMarshaller
import org.grails.web.json.JSONWriter
import org.springframework.stereotype.Component

/**
 * Variant of {@link CollectionMarshaller} that works with {@link Iterator}s
 * instead.
 */
@Component
class IteratorMarshaller implements ObjectMarshaller<JSON> {

    @Override
    boolean supports(Object object) {
        object instanceof Iterator
    }

    @Override
    void marshalObject(Object object, JSON converter) {
        JSONWriter writer = converter.writer
        writer.array()
        ((Iterator) object).each {
            converter.convertAnother it
        }
        writer.endArray()
    }
}
