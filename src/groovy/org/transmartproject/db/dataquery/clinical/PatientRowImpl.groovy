package org.transmartproject.db.dataquery.clinical

import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.core.dataquery.clinical.PatientRow

class PatientRowImpl implements PatientRow {

    Patient patient

    String getLabel() {
        patient.inTrialId
    }

    @Delegate
    DataRow<ClinicalVariableColumn, Object> delegatingDataRow

}
