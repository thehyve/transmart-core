package org.transmartproject.db.dataquery.clinical.variables

import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.clinical.ClinicalVariable
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.core.dataquery.clinical.ComposedVariable

abstract class AbstractComposedVariable implements ComposedVariable {
    List<ClinicalVariable> innerClinicalVariables

    abstract Object getVariableValue(DataRow<ClinicalVariableColumn, Object> clinicalVariableColumn)
}
