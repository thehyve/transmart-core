package org.transmartproject.rest.ontology

import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.Study

class OntologyTermCategory {

    static final String ROOT_CONCEPT_PATH = 'ROOT'

    static String encodeAsURLPart(OntologyTerm term, Study study) {
        // this is correct because keys always end in \ and \ cannot occur
        // as part of a component
        if (!term.key.startsWith(study.ontologyTerm.key)) {
            throw new IllegalArgumentException(
                    "Term $term does not belong to study $study")
        }

        def pathPart = term.key - study.ontologyTerm.key ?: ROOT_CONCEPT_PATH

        // more characters are allowed by RFC 3986, but it doesn't hurt to
        // percent encode extra characters
        pathPart.replaceAll(~/[^a-zA-Z0-9-._\\]/) { String it ->
            it.getBytes('UTF-8').
                    collect {
                        String.format('%%%02x', it)
                    }.join('')
        }.
                replace('\\', '/').
                replaceFirst(~/\\/$/, '') // drop final /
    }

    static String keyFromURLPart(String partArg, Study study) {
        /* the part is already decoded */
        def part = partArg[-1] == '/' ?
                partArg.substring(0, partArg.size() - 1) :
                partArg

        if (part == 'ROOT') {
            return study.ontologyTerm.key
        }

        study.ontologyTerm.key +
                part.replace('/', '\\') + '\\'
    }

}
