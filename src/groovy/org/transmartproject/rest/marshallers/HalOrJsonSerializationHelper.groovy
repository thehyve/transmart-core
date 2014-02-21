package org.transmartproject.rest.marshallers

import grails.rest.Link

interface HalOrJsonSerializationHelper<T> {

    Class<T> getTargetType()

    Collection<Link> getLinks(T object)

    Map<String, Object> convertToMap(T object)

    Set<String> getEmbeddedEntities(T object)

    String getCollectionName()

}
