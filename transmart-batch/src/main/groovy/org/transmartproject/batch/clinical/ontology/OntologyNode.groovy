package org.transmartproject.batch.clinical.ontology

import groovy.transform.Canonical

/**
 * Representation of an entry in the ontology mapping file,
 * which maps variables to ontology terms and contains ancestor
 * nodes for these terms.
 */
@Canonical
class OntologyNode {
    String categoryCode
    String dataLabel

    String code
    String label
    String uri
    List<String> ancestorCodes
}
