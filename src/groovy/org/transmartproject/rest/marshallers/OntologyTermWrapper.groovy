package org.transmartproject.rest.marshallers

import org.transmartproject.core.ontology.OntologyTerm

/**
 * Wraps an OntologyTerm for serialization.
 * Marshallers/Serializers have a static registry where the class is the key, and transSMART already
 * has its own serializer for OntologyTerm, so we use this class to wrap the OntologyTerm and pick the right Serializer
 * for REST API
 */
class OntologyTermWrapper {

    OntologyTerm delegate

    OntologyTermWrapper(OntologyTerm term) {
        this.delegate = term
    }

    static List<OntologyTermWrapper> wrap(List<OntologyTerm> source) {
        source.collect { new OntologyTermWrapper(it) }
    }

    boolean isHighDim() {
        OntologyTerm.VisualAttributes.HIGH_DIMENSIONAL in this.delegate.visualAttributes
    }
}
