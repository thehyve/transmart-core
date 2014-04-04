package org.transmartproject.db.dataquery.clinical

import groovy.transform.ToString
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.core.dataquery.clinical.PatientRow
import org.transmartproject.db.dataquery.clinical.variables.AbstractComposedVariable
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable

@ToString(includes = ['label', 'delegatingDataRow'])
class PatientRowImpl implements PatientRow {

    Patient patient

    String getLabel() {
        patient.inTrialId
    }

    @Delegate
    DataRow<ClinicalVariableColumn, Object> delegatingDataRow

    Object getAt(ClinicalVariableColumn column) {
        /* the delegating data row only knows about TerminalConceptVariables */
        if (column instanceof AbstractComposedVariable) {
            column.getVariableValue(this)
        } else {
            assert column instanceof TerminalConceptVariable
            delegatingDataRow.getAt(column)
        }
    }

}
