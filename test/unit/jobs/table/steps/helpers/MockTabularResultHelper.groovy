package jobs.table.steps.helpers

import org.gmock.GMockController
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.highdim.AssayColumn

class MockTabularResultHelper {

    GMockController gMockController

    List<AssayColumn> createSampleAssays(int n) {
        (1..n).collect {
            createMockAssay("patient_${it}_subject_id",
                    "sample_code_${it}")
        }
    }

    DataRow createRowForAssays(List<AssayColumn> assays,
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


    private AssayColumn createMockAssay(String patientInTrialId, String label) {
        [
                getPatientInTrialId: { -> patientInTrialId },
                getLabel:            { -> label },
                equals:              { other -> delegate.is(other) },
                toString:            { -> "assay for $patientInTrialId" as String }
        ] as AssayColumn
    }

    private DataRow<AssayColumn, Double> createMockRow(Map<AssayColumn, Double> values,
                                                       String label) {
        DataRow row = mock(DataRow)
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
