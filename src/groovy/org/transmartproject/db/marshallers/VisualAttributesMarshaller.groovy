package org.transmartproject.db.marshallers

import org.transmartproject.core.ontology.OntologyTerm.VisualAttributes

class VisualAttributesMarshaller {
    static targetType = VisualAttributes

    def convert(VisualAttributes attribute) {
        attribute.name()
    }
}
