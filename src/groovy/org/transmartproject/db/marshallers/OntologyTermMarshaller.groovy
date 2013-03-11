package org.transmartproject.db.marshallers

import org.transmartproject.core.ontology.OntologyTerm

class OntologyTermMarshaller {

    final targetType = OntologyTerm

    def convert(OntologyTerm term) {
        return [
                level            : term.level,
                fullName         : term.fullName,
                name             : term.name,
                tooltip          : term.tooltip,
                visualAttributes : term.visualAttributes
        ]
    }

}
