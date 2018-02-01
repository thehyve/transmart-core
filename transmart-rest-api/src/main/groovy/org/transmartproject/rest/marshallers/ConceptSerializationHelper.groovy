package org.transmartproject.rest.marshallers

import grails.rest.Link
import org.transmartproject.core.concept.Concept

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF

class ConceptSerializationHelper extends AbstractHalOrJsonSerializationHelper<ConceptWrapper> {

    final Class targetType = ConceptWrapper

    final String collectionName = 'concepts'

    @Override
    Collection<Link> getLinks(ConceptWrapper wrapper) {
        [new Link(RELATIONSHIP_SELF, "/${wrapper.apiVersion}/concepts/${wrapper.concept.conceptCode}")]
    }

    @Override
    Map<String, Object> convertToMap(ConceptWrapper object) {
        Concept concept = object.concept
        [
                conceptCode: concept.conceptCode,
                conceptPath: concept.conceptPath,
                name: concept.name
        ]
    }

}
