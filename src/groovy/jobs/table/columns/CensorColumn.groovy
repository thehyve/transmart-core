package jobs.table.columns

import com.google.common.collect.ImmutableMap
import groovy.transform.CompileStatic
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.core.dataquery.clinical.PatientRow

/**
 * Column for censoring variable. Results in CENSORING_TRUE when the patient
 * matches one of the leaf nodes or CENSORING_FALSE if not.
 */
@CompileStatic
class CensorColumn extends AbstractColumn {

    public static def CENSORING_TRUE = '1'
    public static def CENSORING_FALSE = '0'

    Set<ClinicalVariableColumn> leafNodes

    PatientRow lastRow

    @Override
    void onReadRow(String dataSourceName, Object row) {
        lastRow = (PatientRow) row
    }

    @Override
    Map<String, Object> consumeResultingTableRows() {
        if (!lastRow) return ImmutableMap.of()

        for (clinicalVariable in leafNodes) {
            if (lastRow.getAt(clinicalVariable)) {
                return ImmutableMap.of(getPrimaryKey(lastRow),
                        CENSORING_TRUE)
            }
        }

        ImmutableMap.of(getPrimaryKey(lastRow), CENSORING_FALSE)
    }

    protected String getPrimaryKey(PatientRow row) {
        row.patient.inTrialId
    }
}
