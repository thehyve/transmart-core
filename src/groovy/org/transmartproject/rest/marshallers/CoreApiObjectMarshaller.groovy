package org.transmartproject.rest.marshallers

import grails.converters.JSON
import grails.rest.Link
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller

import static grails.rest.render.util.AbstractLinkingRenderer.*

class CoreApiObjectMarshaller implements ObjectMarshaller<JSON> {

    public static final String LINKS_ATTRIBUTE = '_links'
    public static final String EMBEDDED_ATTRIBUTE = '_embedded'
    HalOrJsonSerializationHelper serializationHelper

    Class<?> getTargetType() {
        serializationHelper.targetType
    }

    @Override
    boolean supports(Object object) {
        serializationHelper.targetType.isAssignableFrom(object.getClass())
    }

    @Override
    void marshalObject(Object object, JSON json) throws ConverterException {
        Map<String, Object> mapRepresentation =
                serializationHelper.convertToMap(object)

        if (json.contentType.contains('hal')) {
            mapRepresentation[LINKS_ATTRIBUTE] =
                    serializationHelper.getLinks(object).collectEntries {
                        [it.rel, convertLink(it)]
                    }

            segregateEmbedded mapRepresentation, object
        }

        json.value mapRepresentation
    }

    Map<String, Object> convertLink(Link link) {
        def res = [(HREF_ATTRIBUTE): link.href]
        if (link.hreflang) {
            res[HREFLANG_ATTRIBUTE] = link.hreflang
        }
        if (link.title) {
            res[TITLE_ATTRIBUTE] = link.title
        }
        if (link.contentType) {
            res[TYPE_ATTRIBUTE] = link.contentType
        }
        if (link.templated) {
            res[TEMPLATED_ATTRIBUTE] = true
        }
        if (link.deprecated) {
            res[DEPRECATED_ATTRIBUTE] = true
        }
        res
    }

    void segregateEmbedded(Map<String, Object> map, Object originalObject) {
        def embedded = serializationHelper.
                getEmbeddedEntities(originalObject).
                collectEntries {
                    def association = map.remove(it)
                    [it, association]
                }

        if (embedded) {
            map[EMBEDDED_ATTRIBUTE] = embedded
        }
    }
}
