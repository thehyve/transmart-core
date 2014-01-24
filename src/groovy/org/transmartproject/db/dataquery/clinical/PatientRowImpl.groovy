package org.transmartproject.db.dataquery.clinical

import groovy.transform.ToString
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.core.dataquery.clinical.PatientRow

@ToString(includes = ['label', 'delegatingDataRow'])
class PatientRowImpl implements PatientRow {

    Patient patient

    String getLabel() {
        patient.inTrialId
    }

    @Delegate
    DataRow<ClinicalVariableColumn, Object> delegatingDataRow

}
