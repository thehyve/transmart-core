package jobs.table

import org.gmock.GMockController
import org.transmartproject.core.dataquery.ColumnOrderAwareDataRow
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn

class MockTabularResultHelper {

    GMockController gMockController

    List<AssayColumn> createSampleAssays(int n) {
        (1..n).collect {
            createMockAssay("patient_${it}_subject_id",
                    "sample_code_${it}")
        }
    }

    ColumnOrderAwareDataRow createRowForAssays(List<AssayColumn> assays,
                                               List<Double> data,
                                               String label) {
        createMockRow(
                dot(assays, data, {a, b -> [ a, b ]})
                        .collectEntries(Closure.IDENTITY),
                label)
    }

    List dot(List list1, List list2, function) {
        def res = []
        for (int i = 0; i < list1.size(); i++) {
            res << function(list1[i], list2[i])
        }
        res
    }

    List<Patient> createPatients(int n) {
        (1..n).collect {
            Patient p = mock(Patient)
            p.inTrialId.returns("subject id #$it".toString()).atLeastOnce()
            p
        }
    }

    List<String> createPatientRowLabels(int n) {
        (1..n).collect {
            "patient #$it" as String
        }
    }

    TabularResult<AssayColumn, Number> createMockTabularResult(Map params) {
        List<AssayColumn> sampleAssays        = params.assays
        Map<String, List<Number>> labelToData = params.data
        String columnsDimensionLabel          = params.columnsLabel
        String rowsDimensionLabel             = params.rowsLabel

        TabularResult highDimResult = mock TabularResult
        highDimResult.indicesList.returns(sampleAssays).atLeastOnce()
        highDimResult.getRows().returns(
                labelToData.collect { String label, List<Number> data ->
                    createRowForAssays(sampleAssays, data, label)
                }.iterator())

        if (columnsDimensionLabel) {
            highDimResult.columnsDimensionLabel.returns columnsDimensionLabel
        }
        if (rowsDimensionLabel) {
            highDimResult.rowsDimensionLabel.returns rowsDimensionLabel
        }

        highDimResult
    }


    private AssayColumn createMockAssay(String patientInTrialId, String label) {
        [
                getPatientInTrialId: { -> patientInTrialId },
                getLabel:            { -> label },
                equals:              { other -> delegate.is(other) },
                toString:            { -> "assay for $patientInTrialId" as String }
        ] as AssayColumn
    }

    private ColumnOrderAwareDataRow<AssayColumn, Double> createMockRow(Map<AssayColumn, Double> values,
                                                                       String label) {
        ColumnOrderAwareDataRow row = mock(ColumnOrderAwareDataRow)
        row.label.returns(label).stub()

        values.eachWithIndex { entry, i ->
            row.getAt(i).returns(entry.value).stub()
        }
        values.keySet().each { column ->
            row.getAt(column).returns(values[column]).stub()
        }
        row.iterator().returns(values.values().iterator()).stub()

        row
    }

    private Object mock(Class clazz) {
        gMockController.mock clazz
    }
}
