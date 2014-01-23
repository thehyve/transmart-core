package jobs.table.columns

import com.google.common.collect.ImmutableMap
import groovy.transform.CompileStatic
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.core.dataquery.clinical.PatientRow

@CompileStatic
class SimpleConceptVariableColumn extends AbstractColumn {

    ClinicalVariableColumn column

    private PatientRow lastRow

    @Override
    void onReadRow(String dataSourceName, Object row) {
        /* calls to onReadRow() are guaranteed to be called interleaved with
         * consumeResultingTableRow */
        assert lastRow == null
        assert row instanceof PatientRow

        lastRow = (PatientRow) row
    }

    @Override
    Map<String, Object> consumeResultingTableRows() {
        if (!lastRow) return ImmutableMap.of()

        def cellValue = lastRow.getAt(column)
        def res

        if (cellValue != null) {
            res = ImmutableMap.of(getPrimaryKey(lastRow), cellValue)
        } else {
            res = ImmutableMap.of()
        }

        lastRow = null
        res
    }

    protected String getPrimaryKey(PatientRow row) {
        lastRow.patient.inTrialId
    }
}
