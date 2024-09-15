package org.transmartproject.rest.marshallers

import grails.rest.Link

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF

class ConceptModifiersSerializationHelper extends AbstractHalOrJsonSerializationHelper<ConceptModifiersWrapper> {

    final Class targetType = ConceptModifiersWrapper

    final String collectionName = 'modifiers'

    @Override
    Map<String, Object> convertToMap(ConceptModifiersWrapper object) {
        [modifiers: object.modifiers]
    }
}
