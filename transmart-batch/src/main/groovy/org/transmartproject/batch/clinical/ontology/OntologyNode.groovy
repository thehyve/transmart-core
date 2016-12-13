package org.transmartproject.batch.clinical.ontology

import groovy.transform.Canonical

@Canonical
class OntologyNode {
    String categoryCode
    String dataLabel

    String code
    String label
    String uri
    List<String> ancestorCodes
}
