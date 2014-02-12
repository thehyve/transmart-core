package org.transmartproject.webservices

import grails.converters.JSON
import org.transmartproject.core.ontology.Study

class StudyJsonMarshaller {

    void register() {
        JSON.registerObjectMarshaller(Study) { Study study ->
            return [
                    name:         study.name,
                    ontologyTerm: study.ontologyTerm
            ]
        }
    }
}
