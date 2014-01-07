package org.transmartproject.db.dataquery.clinical

import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn

class PatientIdAnnotatedDataRow implements DataRow<ClinicalVariableColumn, Object> {

    Long patientId

    List<Object> data

    Map<ClinicalVariableColumn, Integer> columnToIndex

    @Override
    String getLabel() {
        throw new UnsupportedOperationException()
    }

    @Override
    String getAt(int index) {
        data[index]
    }

    @Override
    String getAt(ClinicalVariableColumn column) {
        data[columnToIndex[column]]
    }

    @Override
    Iterator<String> iterator() {
        data.iterator()
    }
}
