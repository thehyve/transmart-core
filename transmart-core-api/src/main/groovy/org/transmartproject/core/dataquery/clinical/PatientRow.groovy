package org.transmartproject.core.dataquery.clinical

import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.doc.Experimental

interface PatientRow extends DataRow<ClinicalVariableColumn, Object> {

    /**
     * Retrieve the patient associated with this row.
     *
     * @return the patient for this row
     */
    Patient getPatient()
}
