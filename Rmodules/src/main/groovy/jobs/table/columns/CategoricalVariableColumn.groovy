package jobs.table.columns

import com.google.common.collect.ImmutableMap
import groovy.transform.CompileStatic
import org.transmartproject.core.dataquery.clinical.ClinicalVariable
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.core.dataquery.clinical.PatientRow

/**
 * Column for categorical variable when implemented as a set of mutually
 * exclusive text {@link ClinicalVariable}s with either take one single
 * value or are empty (as opposed to a single concept that takes several
 * distinct values).
 */
class CategoricalVariableColumn extends AbstractColumn {

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
                                       lastRow.getAt(clinicalVariable))
            }
        }

        ImmutableMap.of()
    }

    protected String getPrimaryKey(PatientRow row) {
        lastRow.patient.inTrialId
    }
}
