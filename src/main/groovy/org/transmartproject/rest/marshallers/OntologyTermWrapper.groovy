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

package org.transmartproject.rest.marshallers

import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTermType

import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.HIGH_DIMENSIONAL
import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.LEAF

/**
 * Wraps an OntologyTerm for serialization.
 * Marshallers/Serializers have a static registry where the class is the key, and transSMART already
 * has its own serializer for OntologyTerm, so we use this class to wrap the OntologyTerm and pick the right Serializer
 * for REST API.
 *
 * We also use it for some helper methods.
 */
class OntologyTermWrapper {

    OntologyTerm delegate

    private boolean root = false

    OntologyTermWrapper(OntologyTerm term, boolean root) {
        this.delegate = term
        this.root = root
    }

    boolean isHighDim() {
        HIGH_DIMENSIONAL in delegate.visualAttributes
    }

    OntologyTermType getOntologyTermType() {
        if (highDim) {
            OntologyTermType.HIGH_DIMENSIONAL
        } else if (root) {
            OntologyTermType.STUDY
        } else if (delegate.metadata?.okToUseValues) {
            OntologyTermType.NUMERIC
        } else if (LEAF in delegate.visualAttributes) {
            OntologyTermType.CATEGORICAL_OPTION
        } else {
            OntologyTermType.UNKNOWN
        }
    }
}
