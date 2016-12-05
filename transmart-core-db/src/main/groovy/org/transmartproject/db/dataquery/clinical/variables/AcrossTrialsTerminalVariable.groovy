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

package org.transmartproject.db.dataquery.clinical.variables

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.transmartproject.core.concept.ConceptKey
import org.transmartproject.core.dataquery.DataColumn
import static org.transmartproject.db.ontology.AbstractAcrossTrialsOntologyTerm.ACROSS_TRIALS_TABLE_CODE

@EqualsAndHashCode(includes = [ 'modifierCode', 'conceptPath' ])
@ToString
class AcrossTrialsTerminalVariable implements TerminalClinicalVariable, DataColumn {

    public final static String GROUP_NAME = this.simpleName

    String conceptPath

    /* to be filled by AcrossTrialsDataQuery */
    String modifierCode

    @Override
    String getLabel() {
        conceptPath
    }

    @Override
    String getGroup() {
        GROUP_NAME
    }

    @Override
    String getCode() {
        modifierCode
    }

    @Override
    ConceptKey getKey() {
        new ConceptKey(ACROSS_TRIALS_TABLE_CODE, conceptPath)
    }
}
