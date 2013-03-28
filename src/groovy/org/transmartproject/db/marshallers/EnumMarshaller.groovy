package org.transmartproject.db.marshallers

import org.transmartproject.core.ontology.OntologyTerm.VisualAttributes

class EnumMarshaller {
    static targetType = Enum

    def convert(Enum enumeration) {
        enumeration.name()
    }
}
