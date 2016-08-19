/*
 * Copyright 2014 Janssen Research & Development, LLC.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

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
