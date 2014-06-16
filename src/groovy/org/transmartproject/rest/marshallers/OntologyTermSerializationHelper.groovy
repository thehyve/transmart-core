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
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.rest.StudyLoadingService

import javax.annotation.Resource

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF

class OntologyTermSerializationHelper implements HalOrJsonSerializationHelper<OntologyTermWrapper> {

    @Resource
    StudyLoadingService studyLoadingServiceProxy

    final Class targetType = OntologyTermWrapper

    final String collectionName = 'ontology_terms'

    @Override
    Collection<Link> getLinks(OntologyTermWrapper obj) {
        OntologyTerm term = obj.delegate
        String url = studyLoadingServiceProxy.getOntologyTermUrl(obj.delegate)

        Link datalink
        if (obj.isHighDim()) {
            datalink = new Link('highdim', HighDimSummarySerializationHelper.getHighDimIndexUrl(url))
        } else {
            datalink = new Link('observations', ObservationSerializationHelper.getObservationsIndexUrl(url))
        }

        [
                // TODO add other relationships (children, parent, ...)
                new Link(RELATIONSHIP_SELF, url),
                datalink
        ]
    }

    @Override
    Map<String, Object> convertToMap(OntologyTermWrapper obj) {
        OntologyTerm term = obj.delegate
        [
                name:     term.name,
                key:      term.key,
                fullName: term.fullName,
        ]
    }

    @Override
    Set<String> getEmbeddedEntities(OntologyTermWrapper object) {
        [] as Set
    }

}
