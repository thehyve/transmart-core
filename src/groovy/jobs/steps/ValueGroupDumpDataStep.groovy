package jobs.steps

import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow

class ValueGroupDumpDataStep extends AbstractDumpHighDimensionalDataStep {

    @Override
    protected computeCsvRow(String subsetName,
                            String seriesName,
                            DataRow row,
                            AssayColumn column,
                            Object cell) {
        [
                getRowKey(subsetName, seriesName, column.patientInTrialId),
                row[column],
                row.label,
                (row instanceof BioMarkerDataRow) ?
                    row.bioMarker : ""
        ]
    }

    final List<String> csvHeader = [ 'PATIENT_NUM', 'VALUE', 'GROUP', 'GENE_SYMBOL' ]
}
