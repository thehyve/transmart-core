package org.transmartproject.batch.ontologymapping

import groovy.transform.Canonical
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.stereotype.Component
import org.transmartproject.ontology.OntologyMap

/**
 * Class for storing ontology map nodes between fetching and writing.
 */
@Component
@JobScope
class OntologyMappingHolder {

    @Canonical
    static class Key {
        String ontologyCode
        String categoryCode
        String dataLabel
    }

    Map<Key, OntologyMap> nodes

}
