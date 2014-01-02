package jobs.table.columns

import com.google.common.collect.ImmutableMap
import groovy.transform.CompileStatic
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.core.dataquery.clinical.PatientRow

@CompileStatic
class SimpleConceptVariableColumn extends AbstractColumn {

    final boolean globalComputation = false

    ClinicalVariableColumn column

    PatientRow lastRow

    @Override
    void onReadRow(String dataSourceName, Object row) {
        assert row instanceof PatientRow

        lastRow = (PatientRow) row
    }

    @Override
    Map<String, String> consumeResultingTableRows() {
        /* if we only subscribe one source, as we should, there calls to
         * onReadRow() are guaranteed to be called interleaved with
         * consumeResultingTableRow */
        ImmutableMap.of(getPrimaryKey(lastRow), (String) lastRow.getAt(column))
    }

    protected String getPrimaryKey(PatientRow row) {
        lastRow.patient.inTrialId
    }
}
