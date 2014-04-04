package org.transmartproject.rest.marshallers

import grails.rest.Link

class ObservationSerializationHelper implements HalOrJsonSerializationHelper<ObservationWrapper> {

    final Class targetType = ObservationWrapper

    final String collectionName = 'observations'

    @Override
    Map<String, Object> convertToMap(ObservationWrapper observation) {
        [
                subject: observation.subject,
                label:   observation.label,
                value:   observation.value,
        ]
    }

    @Override
    Set<String> getEmbeddedEntities(ObservationWrapper observation) {
        ['subject'] as Set
    }

    @Override
    Collection<Link> getLinks(ObservationWrapper observation) {
        [] as Collection
    }
}
