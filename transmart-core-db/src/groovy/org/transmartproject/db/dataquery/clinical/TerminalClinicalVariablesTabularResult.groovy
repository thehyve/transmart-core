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

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.Maps
import groovy.transform.CompileStatic
import org.hibernate.ScrollableResults
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.db.dataquery.CollectingTabularResult
import org.transmartproject.db.dataquery.clinical.variables.TerminalClinicalVariable
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable

@CompileStatic
class TerminalClinicalVariablesTabularResult extends
        CollectingTabularResult<TerminalClinicalVariable, PatientIdAnnotatedDataRow> {

    public static final String TEXT_VALUE_TYPE = 'T'

    public static final int PATIENT_NUM_COLUMN_INDEX  = 0
    public static final int CODE_COLUMN_INDEX         = 1
    public static final int VALUE_TYPE_COLUMN_INDEX   = 2
    public static final int TEXT_VALUE_COLUMN_INDEX   = 3
    public static final int NUMBER_VALUE_COLUMN_INDEX = 4

    /* XXX: this class hierarchy needs some refactoring, we're depending on
     * implementation details of CollectingTabularResults and skipping quite
     * some logic from it (see below the assignment for allowMissingColumn and
     * the overriding of finalizeCollectedEntries()).
     * Adding a new superclass for CollectingTabularResults and extending that
     * instead would be a simple (but perhaps not very elegant) solution.
     */

    BiMap<TerminalClinicalVariable, Integer> localIndexMap = HashBiMap.create()

    /* variant of above map with variables replaced with their concept code */
    private Map<String, Integer> codeToIndex = Maps.newHashMap()

    final String variableGroup

    TerminalClinicalVariablesTabularResult(ScrollableResults results,
                                          List<TerminalClinicalVariable> indicesList) {
        this.results = results

        this.indicesList = indicesList

        this.indicesList.each { TerminalClinicalVariable it ->
            localIndexMap[it] = indicesList.indexOf it
        }

        localIndexMap.each { TerminalClinicalVariable var, Integer index ->
            codeToIndex[var.code] = index
        }

        if (indicesList.empty) {
            throw new InvalidArgumentsException("Indices list is empty")
        }

        def groups = indicesList*.group.unique()
        if (groups.size() != 1) {
            throw new InvalidArgumentsException("Expected all the clinical " +
                    "variables in this sub-result to have the same type, " +
                    "found these: $groups")
        }
        this.variableGroup = groups[0]

        /* ** */
        columnsDimensionLabel = 'Clinical Variables'
        rowsDimensionLabel    = 'Patients'
        // actually yes, but this skips the complex logic in
        // addToCollectedEntries() and just adds the row to the list
        allowMissingColumns   = false

        columnIdFromRow = { Object[] row ->
            row[CODE_COLUMN_INDEX]
        }
        inSameGroup = { Object[] row1,
                        Object[] row2 ->
            row1[PATIENT_NUM_COLUMN_INDEX] == row2[PATIENT_NUM_COLUMN_INDEX]
        }

        finalizeGroup = this.&finalizePatientGroup

        /* session is managed outside, in ClinicalDataTabularResult */
        closeSession = false
    }


    final String columnEntityName = 'concept'

    @Override
    protected Object getIndexObjectId(TerminalConceptVariable object) {
        object.conceptCode
    }

    protected void finalizeCollectedEntries(ArrayList collectedEntries) {
        /* nothing to do here. All the logic in finalizePatientGroup */
    }

    private PatientIdAnnotatedDataRow finalizePatientGroup(List<Object[]> list) {
        Map<Integer, TerminalClinicalVariable> indexToColumn = localIndexMap.inverse()

        Object[] transformedData = new Object[localIndexMap.size()]

        /* don't take Object[] otherwise would be vararg func and
         * further unwrapping needed */
        list.each { Object rawRowUntyped ->
            /* array with 5 elements */
            if (!rawRowUntyped) {
                return
            }
            Object[] rawRow = (Object[])rawRowUntyped

            /* find out the position of this concept in the final result */
            Integer index = codeToIndex[rawRow[CODE_COLUMN_INDEX] as String]
            if (index == null) {
                throw new IllegalStateException("Unexpected concept code " +
                        "'${rawRow[CODE_COLUMN_INDEX]}' at this point; " +
                        "expected one of ${codeToIndex.keySet()}")
            }

            /* and the corresponding variable */
            TerminalClinicalVariable var = indexToColumn[index]

            if (transformedData[index] != null) {
                throw new UnexpectedResultException("Got more than one fact for " +
                        "patient ${rawRow[PATIENT_NUM_COLUMN_INDEX]} and " +
                        "code $var.code. This is currently unsupported")
            }

            transformedData[index] = getVariableValue(rawRow)
        }

        new PatientIdAnnotatedDataRow(
                patientId:     (list.find { it != null})[PATIENT_NUM_COLUMN_INDEX] as Long,
                data:          Arrays.asList(transformedData) as List,
                columnToIndex: localIndexMap as Map)
    }

    private Object getVariableValue(Object[] rawRow) {
        String valueType = rawRow[VALUE_TYPE_COLUMN_INDEX]

        if (valueType == TEXT_VALUE_TYPE) {
            rawRow[TEXT_VALUE_COLUMN_INDEX]
        } else {
            rawRow[NUMBER_VALUE_COLUMN_INDEX]
        }
    }
}
