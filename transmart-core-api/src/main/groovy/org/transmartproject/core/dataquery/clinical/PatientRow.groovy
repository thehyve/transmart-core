package org.transmartproject.core.dataquery.clinical

import org.transmartproject.core.dataquery.ColumnOrderAwareDataRow
import org.transmartproject.core.dataquery.Patient

interface PatientRow extends ColumnOrderAwareDataRow<ClinicalVariableColumn, Object> {

    /**
     * Retrieve the patient associated with this row.
     *
     * @return the patient for this row
     */
    Patient getPatient()
}
