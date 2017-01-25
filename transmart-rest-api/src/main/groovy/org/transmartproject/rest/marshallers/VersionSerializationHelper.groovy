package org.transmartproject.rest.marshallers

import grails.rest.Link

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF

class VersionSerializationHelper extends AbstractHalOrJsonSerializationHelper<VersionWrapper> {

    final Class targetType = VersionWrapper

    final String collectionName = 'versions'

    @Override
    Collection<Link> getLinks(VersionWrapper version) {
        [new Link(RELATIONSHIP_SELF, "/versions/${version.id}")]
    }

    @Override
    Map<String, Object> convertToMap(VersionWrapper version) {
        [
                id: version.id,
                prefix: version.prefix
        ]
    }

}
