package org.transmartproject.rest.marshallers

import org.transmartproject.core.ontology.Study

class StudyMarshaller {

    static targetType = Study

    def convert(Study study) {
        [
                name:         study.name,
                ontologyTerm: study.ontologyTerm
        ]
    }

}
