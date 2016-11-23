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

import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.concept.ConceptKey

import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.FOLDER
import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.LEAF

class AcrossTrialsOntologyTerm extends AbstractAcrossTrialsOntologyTerm {

    ModifierDimensionView modifierDimension

    @Override
    Integer getLevel() {
        modifierDimension.level + 1 /* because of ACROSS_TRIALS_BEFORE */
    }

    @Override
    String getKey() {
        new ConceptKey(ACROSS_TRIALS_TABLE_CODE, fullName)
    }

    @Override
    String getFullName() {
        "\\${ACROSS_TRIALS_TOP_TERM_NAME}${modifierDimension.path}"
    }

    @Override
    String getName() {
        modifierDimension.name
    }

    @Override
    String getCode() {
        modifierDimension.code
    }

    @Override
    String getTooltip() {
        name
    }

    @Override
    EnumSet<OntologyTerm.VisualAttributes> getVisualAttributes() {
        EnumSet.of(modifierDimension.nodeType == 'F' ?
                FOLDER : LEAF)
    }

    @Override
    Object getMetadata() {
        [okToUseValues: modifierDimension.valueType == 'N']
    }
}
