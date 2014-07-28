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
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn

@EqualsAndHashCode(includes = [ 'conceptCode', 'conceptPath' ])
@ToString
class TerminalConceptVariable implements ClinicalVariableColumn {

    public static final String TEXT_VALUE_TYPE = 'T'

    public static final int PATIENT_NUM_COLUMN_INDEX  = 0
    public static final int CONCEPT_CODE_COLUMN_INDEX = 1
    public static final int VALUE_TYPE_COLUMN_INDEX   = 2
    public static final int TEXT_VALUE_COLUMN_INDEX   = 3
    public static final int NUMBER_VALUE_COLUMN_INDEX = 4

    /* when created, only one needs to be filled, but then a postprocessing
     * step must fill the other */
    String conceptCode,
           conceptPath

    Object getVariableValue(Object[] row) {
        String valueType = row[VALUE_TYPE_COLUMN_INDEX]

        if (valueType == TEXT_VALUE_TYPE) {
            row[TEXT_VALUE_COLUMN_INDEX]
        } else {
            row[NUMBER_VALUE_COLUMN_INDEX]
        }
    }

    @Override
    String getLabel() {
        conceptPath
    }
}
