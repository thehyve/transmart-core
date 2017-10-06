package jobs.steps

import org.transmartproject.core.dataquery.ColumnOrderAwareDataRow
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow

class BioMarkerDumpDataStep extends AbstractDumpHighDimensionalDataStep {

    @Override
    protected computeCsvRow(String subsetName,
                            String seriesName,
                            ColumnOrderAwareDataRow row,
                            AssayColumn column,
                            Object cell) {

        assert row instanceof BioMarkerDataRow

        [
                getRowKey(subsetName, seriesName, column.patientInTrialId),
                row[column],
                row.label,
                row.bioMarker,
                subsetName
        ]
    }

    final List<String> csvHeader =
        [ 'PATIENT.ID', 'VALUE', 'PROBE.ID', 'GENE_SYMBOL', 'SUBSET' ]

}
