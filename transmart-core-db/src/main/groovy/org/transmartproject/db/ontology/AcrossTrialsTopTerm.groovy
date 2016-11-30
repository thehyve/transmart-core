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

class AcrossTrialsTopTerm extends AbstractAcrossTrialsOntologyTerm {

    public final static String ACROSS_TRIALS_TERM_CODE = '-99'

    /**
     * May cause some issues in versions where the initial level is -1
     */
    final Integer level = 0

    final String fullName = "\\${ACROSS_TRIALS_TOP_TERM_NAME}\\"

    final String name = ACROSS_TRIALS_TOP_TERM_NAME

    final String code = ACROSS_TRIALS_TERM_CODE

    final EnumSet<OntologyTerm.VisualAttributes> visualAttributes =
            EnumSet.of(OntologyTerm.VisualAttributes.CONTAINER)

    final Object metadata = null
}
