package org.transmartproject.rest.marshallers

import grails.rest.Link
import org.transmartproject.rest.VersionController.VersionWrapper

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF

class VersionSerializationHelper extends AbstractHalOrJsonSerializationHelper<VersionWrapper> {

    final Class targetType = VersionWrapper

    final String collectionName = 'versions'

    @Override
    Collection<Link> getLinks(VersionWrapper version) {
        [new Link(RELATIONSHIP_SELF, "/versions/${version.version.id}")]
    }

    @Override
    Map<String, Object> convertToMap(VersionWrapper version) {
        version.version as LinkedHashMap
    }

}
