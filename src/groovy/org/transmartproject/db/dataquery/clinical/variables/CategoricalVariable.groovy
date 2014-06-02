package org.transmartproject.db.dataquery.clinical.variables

import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.core.exceptions.UnexpectedResultException

class CategoricalVariable extends AbstractComposedVariable implements
        ClinicalVariableColumn {

    String conceptPath

    @Override
    String getVariableValue(DataRow<ClinicalVariableColumn, Object> dataRow) {
        for (var in innerClinicalVariables) {
            def currentValue = dataRow.getAt(var)

            if (currentValue) {
                if (!(currentValue instanceof String)) {
                    throw new UnexpectedResultException("Expected a string " +
                            "for observation in row $dataRow, categorical " +
                            "variable $this, when looking at child variable " +
                            "$var. Instead, got '$currentValue'")
                }

                return currentValue
            }
        }

        null
    }

    @Override
    String getLabel() {
        conceptPath
    }
}
