package org.transmartproject.webservices

import grails.converters.JSON
import org.transmartproject.core.ontology.OntologyTerm

class OntologyTermJsonMarshaller {

    void register() {
        JSON.registerObjectMarshaller(OntologyTerm) { OntologyTerm term ->
            return [
                    name:     term.name,
                    key:      term.key,
                    fullName: term.fullName,
            ]
        }
    }
}
