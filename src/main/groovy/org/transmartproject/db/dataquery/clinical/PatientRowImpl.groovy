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

package org.transmartproject.db.dataquery.clinical

import com.google.common.collect.Iterators
import groovy.transform.ToString
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.core.dataquery.clinical.PatientRow
import org.transmartproject.db.dataquery.clinical.variables.AbstractComposedVariable
import org.transmartproject.db.dataquery.clinical.variables.TerminalClinicalVariable

@ToString(includes = ['label', 'delegatingDataRow'])
class PatientRowImpl implements PatientRow {

    Patient patient

    List<TerminalClinicalVariable> flattenedIndices

    Map<String, DataRow<ClinicalVariableColumn, Object>> delegatingDataRows

    String getLabel() {
        patient.inTrialId
    }

    @Override
    Object getAt(int index) {
        getAt(flattenedIndices[index])
    }

    @Override
    Object getAt(ClinicalVariableColumn column) {
        /* the delegating data row only knows about TerminalConceptVariables */
        if (column instanceof AbstractComposedVariable) {
            column.getVariableValue(this)
        } else {
            assert column instanceof TerminalClinicalVariable
            delegatingDataRows[column.group].getAt(column)
        }
    }

    @Override
    Iterator<Object> iterator() {
        def innerIterators = delegatingDataRows.values()*.iterator()
        Iterators.concat(innerIterators.iterator())
    }
}
