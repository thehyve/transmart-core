/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.ontology

import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.OntologyTermsResource
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.concept.ConceptKey

import static org.transmartproject.db.ontology.AbstractAcrossTrialsOntologyTerm.ACROSS_TRIALS_TABLE_CODE
import static org.transmartproject.db.ontology.AbstractAcrossTrialsOntologyTerm.ACROSS_TRIALS_TOP_TERM_NAME

/**
 * Across trial functionality by means of special 'Across trial' nodes and modifiers
 * is deprecated.
 * Instead, patients, concepts and nodes are shared by default and observations can be associated
 * with studies through the trial visit dimension (@see {@link org.transmartproject.db.i2b2data.TrialVisit}).
 * Tree nodes can still be made study specific (@see {@link I2b2Secure}).
 */
@Deprecated
class AcrossTrialsConceptsResourceDecorator implements OntologyTermsResource {

    OntologyTermsResource inner

    private final OntologyTerm topTerm = new AcrossTrialsTopTerm()

    @Override
    List<OntologyTerm> getAllCategories() {
        [topTerm] + inner.allCategories
    }

    @Override
    OntologyTerm getByKey(String conceptKey) throws NoSuchResourceException {
        def conceptKeyObj = new ConceptKey(conceptKey)

        if (conceptKeyObj.tableCode != ACROSS_TRIALS_TABLE_CODE) {
            return inner.getByKey(conceptKey)
        }

        def fullName = conceptKeyObj.conceptFullName
        if (fullName[0] != ACROSS_TRIALS_TOP_TERM_NAME) {
            throw new NoSuchResourceException("All the across trials terms' " +
                    "first path component should be " +
                    "${ACROSS_TRIALS_TOP_TERM_NAME}")
        }

        if (fullName.length == 1) {
            topTerm
        } else { // > 1
            String modifier_path = "\\${fullName[1..-1].join '\\'}\\"
            def modifier = ModifierDimensionView.get(modifier_path)
            if (!modifier) {
                throw new NoSuchResourceException('Could not find across ' +
                        "trials node with modifier_path $modifier_path")
            }

            new AcrossTrialsOntologyTerm(modifierDimension: modifier)
        }
    }
}
