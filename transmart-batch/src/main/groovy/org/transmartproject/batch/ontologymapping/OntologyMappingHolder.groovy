package org.transmartproject.batch.ontologymapping

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.ontology.OntologyNode

@Component
@JobScope
class OntologyMappingHolder {
    List<OntologyNode> nodes
}
