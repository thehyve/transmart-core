/*
 * Copyright 2014 Janssen Research & Development, LLC.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.rest.marshallers

import grails.converters.JSON
import grails.rest.Link
import groovy.util.logging.Slf4j
import org.grails.web.converters.marshaller.ObjectMarshaller

import static grails.rest.render.util.AbstractLinkingRenderer.*

@Slf4j
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
    void marshalObject(Object object, JSON json) {
        Map<String, Object> mapRepresentation =
                serializationHelper.convertToMap(object)

        if (json.contentType.contains('hal')) {
            mapRepresentation[LINKS_ATTRIBUTE] = getLinks(object)
            segregateEmbedded mapRepresentation, object
        }

        json.value mapRepresentation
    }

    /**
     * @param object
     * @return map of relationship to link value. Value is either a Link (simple) or a List<Link> (aggregated)
     */
    private Map<String, Object> getLinks(Object object) {

        Map<String, Object> result = [:]
        Map<String, List<Link>> grouped = serializationHelper.getLinks(object).groupBy { it.rel }

        grouped.each {
            key, list ->
                if (serializationHelper.aggregatedLinkRelations.contains(key)) {
                    result.put(key, list.collect { convertLink(it) })
                } else {
                    //only the first element will be picked. Its not supposed to have more than one anyway
                    result.put(key, convertLink(list.get(0)))
                }
        }

        result
    }

    private Map<String, Object> convertLink(Link link) {
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

    private void segregateEmbedded(Map<String, Object> map, Object originalObject) {
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
