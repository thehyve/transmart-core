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
import org.transmartproject.core.concept.ConceptFullName
import org.transmartproject.core.concept.ConceptKey
import org.transmartproject.core.ontology.OntologyTermsResource
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTermTag
import org.transmartproject.core.ontology.OntologyTermTagsResource
import org.transmartproject.rest.StudyLoadingService

import javax.annotation.Resource

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF

class OntologyTermSerializationHelper extends AbstractHalOrJsonSerializationHelper<OntologyTermWrapper> {

    @Resource
    StudyLoadingService studyLoadingServiceProxy

    @Resource
    OntologyTermsResource ontologyTermsResourceService

    @Resource
    OntologyTermTagsResource tagsResource

    final Class targetType = OntologyTermWrapper

    final String collectionName = 'ontology_terms'

    public static final RELATIONSHIP_CHILDREN = "children"

    @Override
    Collection<Link> getLinks(OntologyTermWrapper obj) {
        String url = studyLoadingServiceProxy.getOntologyTermUrl(obj.delegate)

        List<Link> result = []
        result.add(new Link(RELATIONSHIP_SELF, url))

        Link datalink
        if (obj.isHighDim()) {
            datalink = new Link('highdim', HighDimSummarySerializationHelper.getHighDimIndexUrl(url))
        } else {
            datalink = new Link('observations', ObservationSerializationHelper.getObservationsIndexUrl(url))
        }

        result.add(datalink)

        OntologyTerm concept = obj.delegate
        for (OntologyTerm ot: concept.children) {
            Link link = new Link(RELATIONSHIP_CHILDREN, studyLoadingServiceProxy.getOntologyTermUrl(ot))
            link.setTitle(ot.name)
            result.add(link)
        }

        def parent = getParent(concept)
        if (parent) {
            result.add(new Link("parent", studyLoadingServiceProxy.getOntologyTermUrl(parent)))
        }
        result
    }

    private OntologyTerm getParent(OntologyTerm term) {
        if (term.level < 2) {
            return null
        }

        def currentKey = new ConceptKey(term.key)
        def key = new ConceptKey(currentKey.tableCode, new ConceptFullName(term.fullName).parent.toString()).toString()
        ontologyTermsResourceService.getByKey(key)
    }

    @Override
    Map<String, Object> convertToMap(OntologyTermWrapper obj) {
        OntologyTerm term = obj.delegate
        def result =
            [
                    name:     term.name,
                    key:      term.key,
                    fullName: term.fullName,
                    type:     obj.ontologyTermType.name(),
                    patientCount: term.getPatientCount()
            ]

        Map<OntologyTerm, List<OntologyTermTag>> map = tagsResource.getTags([term] as Set, false)
        List<OntologyTermTag> tags = map.get(term)

        if (tags && tags.size() > 0) {
            result.put('metadata', tags.collectEntries { [(it.name): it.description ] })
        }

        result
    }

    @Override
    Set<String> getAggregatedLinkRelations() {
        [RELATIONSHIP_CHILDREN] as Set
    }


}
