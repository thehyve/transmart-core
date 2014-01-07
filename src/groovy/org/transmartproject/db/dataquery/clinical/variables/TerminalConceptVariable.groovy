package org.transmartproject.db.dataquery.clinical.variables

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn

@EqualsAndHashCode(includes = [ 'conceptCode' ])
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

    String getVariableValue(Object[] row) {
        String valueType = row[VALUE_TYPE_COLUMN_INDEX]

        if (valueType == TEXT_VALUE_TYPE) {
            row[TEXT_VALUE_COLUMN_INDEX]
        } else {
            row[NUMBER_VALUE_COLUMN_INDEX] as String
        }
    }

    @Override
    String getLabel() {
        conceptPath
    }
}
