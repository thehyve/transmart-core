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
