package org.transmartproject.rest.marshallers

import grails.rest.Link
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

class CollectionResponseWrapperSerializationHelper implements HalOrJsonSerializationHelper<CollectionResponseWrapper> {

    @Autowired
    ApplicationContext ctx

    final Class<CollectionResponseWrapper> targetType = CollectionResponseWrapper

    final String collectionName = 'collection'

    HalOrJsonSerializationHelper findComponentTypeHelper(Class targetComponentType) {
        // TODO cache this
        ctx.getBeansOfType(HalOrJsonSerializationHelper).values().find {
            it.targetType == targetComponentType
        }
    }

    @Override
    Collection<Link> getLinks(CollectionResponseWrapper object) {
        object.links
    }

    @Override
    Map<String, Object> convertToMap(CollectionResponseWrapper object) {
        String key = getKeyForObjectType(object.componentType)
        [
                (key): object.collection
        ]
    }

    @Override
    Set<String> getEmbeddedEntities(CollectionResponseWrapper object) {
        [getKeyForObjectType(object.componentType)] as Set
    }

    private String getKeyForObjectType(Class targetComponentType) {
        HalOrJsonSerializationHelper helper = findComponentTypeHelper(targetComponentType)
        helper?.collectionName ?: 'values'
    }
}
