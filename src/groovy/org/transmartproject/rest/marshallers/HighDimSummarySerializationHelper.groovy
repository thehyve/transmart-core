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

import grails.rest.Link
import org.transmartproject.rest.StudyLoadingService

import javax.annotation.Resource

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF
import static org.transmartproject.rest.marshallers.MarshallerSupport.getPropertySubsetForSuperType

class HighDimSummarySerializationHelper implements HalOrJsonSerializationHelper<HighDimSummary> {

    @Resource
    StudyLoadingService studyLoadingServiceProxy

    Class targetType = HighDimSummary

    String collectionName = 'dataTypes'

    @Override
    Collection<Link> getLinks(HighDimSummary object) {
        String conceptUrl = studyLoadingServiceProxy.getOntologyTermUrl(object.conceptWrapper.delegate)
        String self = getHighDimDataUrl(conceptUrl, object.name)

        List result = [
                new Link(RELATIONSHIP_SELF, self),
        ]

        result.addAll(object.supportedProjections.collect { new Link(it, "${self}&projection=${it}") })
        result
    }

    @Override
    Map<String, Object> convertToMap(HighDimSummary object) {
        getPropertySubsetForSuperType(object, HighDimSummary, ['conceptWrapper','class'] as Set)
    }

    @Override
    Set<String> getEmbeddedEntities(HighDimSummary object) {
        return [] as Set
    }

    static String getHighDimIndexUrl(String conceptUrl) {
        "${conceptUrl}/highdim"
    }

    static String getHighDimDataUrl(String conceptUrl, String dataType) {
        "${conceptUrl}/highdim?dataType=${dataType}"
    }

}
