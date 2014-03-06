package org.transmartproject.rest.marshallers

import grails.rest.Link

class ObservationSerializationHelper implements HalOrJsonSerializationHelper<ObservationWrapper> {

    final Class targetType = ObservationWrapper

    final String collectionName = 'observations'

    @Override
    Map<String, Object> convertToMap(ObservationWrapper observation) {
        [
                patientId: observation.patientId,
                conceptCode: observation.conceptCode,
                value: observation.value,
        ]
    }

    @Override
    Set<String> getEmbeddedEntities(ObservationWrapper observation) {
        return null
    }

    @Override
    String getCollectionName() {
        return null
    }

    @Override
    Collection<Link> getLinks(ObservationWrapper observation) {
        [] as Collection
    }
}
