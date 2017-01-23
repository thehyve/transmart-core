package org.transmartproject.rest.marshallers

import grails.rest.Link

/**
 * Abstract impl of HalOrJsonSerializationHelper, just to implement common defaults and absorb future interface changes
 */
abstract class AbstractHalOrJsonSerializationHelper<T> implements HalOrJsonSerializationHelper<T> {

    @Override
    Collection<Link> getLinks(T object) {
        [] as Collection
    }

    @Override
    Set<String> getEmbeddedEntities(T object) {
        [] as Set
    }

    @Override
    Set<String> getAggregatedLinkRelations() {
        [] as Set
    }
}
