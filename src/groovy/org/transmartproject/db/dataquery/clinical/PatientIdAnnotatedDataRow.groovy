package org.transmartproject.db.dataquery.clinical

import groovy.transform.ToString
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn

@ToString(includes = ['patientId', 'data'])
class PatientIdAnnotatedDataRow implements DataRow<ClinicalVariableColumn, Object> {

    Long patientId

    List<Object> data

    Map<ClinicalVariableColumn, Integer> columnToIndex

    @Override
    String getLabel() {
        throw new UnsupportedOperationException()
    }

    @Override
    Object getAt(int index) {
        data[index]
    }

    @Override
    Object getAt(ClinicalVariableColumn column) {
        data[columnToIndex[column]]
    }

    @Override
    Iterator<Object> iterator() {
        data.iterator()
    }
}
