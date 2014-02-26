package org.transmartproject.rest.marshallers

import grails.rest.Link
import org.transmartproject.db.i2b2data.ObservationFact

class ObservationFactSerializationHelper implements HalOrJsonSerializationHelper<ObservationFact> {

    final Class targetType = ObservationFact

    final String collectionName = 'observations'

    @Override
    Collection<Link> getLinks(ObservationFact observationFact) {
        [] as Collection
    }

    @Override
    Map<String, Object> convertToMap(ObservationFact observationFact) {
        [conceptCode: observationFact.conceptCode,
                valueType: observationFact.valueType,
                numberValue: observationFact.numberValue,
                textValue: observationFact.textValue,
                valueFlag: observationFact.valueFlag,
                sourcesystemCd: observationFact.sourcesystemCd]
    }

    @Override
    Set<String> getEmbeddedEntities(ObservationFact observationFact) {
        [] as Set
    }

}
