package org.transmartproject.rest.marshallers

import grails.rest.Link

class ObservationSerializationHelper implements HalOrJsonSerializationHelper<ObservationWrapper> {

    final Class targetType = ObservationWrapper

    final String collectionName = 'observations'

    @Override
    Map<String, Object> convertToMap(ObservationWrapper observation) {
        [
                subject: observation.subject,
                concept: observation.concept,
                value: observation.value,
        ]
    }

    @Override
    Set<String> getEmbeddedEntities(ObservationWrapper observation) {
        ['subject', 'concept'] as Set
    }

    @Override
    Collection<Link> getLinks(ObservationWrapper observation) {
        [] as Collection
    }
}
