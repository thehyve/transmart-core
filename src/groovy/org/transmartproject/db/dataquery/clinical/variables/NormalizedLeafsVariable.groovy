package org.transmartproject.db.dataquery.clinical.variables

import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn

class NormalizedLeafsVariable extends AbstractComposedVariable
        implements ClinicalVariableColumn {

    String conceptPath

    @Override
    Object getVariableValue(DataRow<ClinicalVariableColumn, Object> dataRow) {
        innerClinicalVariables.collectEntries { var ->
            [var, dataRow.getAt(var)]
        }
    }

    @Override
    String getLabel() {
        conceptPath
    }
}
