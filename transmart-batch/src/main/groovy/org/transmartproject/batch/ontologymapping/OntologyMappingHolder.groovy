package org.transmartproject.batch.ontologymapping

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.stereotype.Component
import org.transmartproject.ontology.OntologyMap

@Component
@JobScope
class OntologyMappingHolder {
    List<OntologyMap> nodes
}
