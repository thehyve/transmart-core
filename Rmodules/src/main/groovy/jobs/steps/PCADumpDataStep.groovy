package jobs.steps

import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow

class PCADumpDataStep extends BioMarkerDumpDataStep {

    @Override
    protected computeCsvRow(String subsetName,
                            String seriesName,
                            DataRow row,
                            AssayColumn column,
                            Object cell) {

        assert row instanceof BioMarkerDataRow

        // Determine which values to put in rowKey and which in probeValue
        String rowKey;
        String probeValue;
        if (params.doUseExperimentAsVariable == "true") {
            rowKey = [subsetName, row.label, column.patientInTrialId].join("_")
            probeValue = seriesName
        }
        else {
            rowKey = [subsetName, seriesName, column.patientInTrialId].join("_")
            probeValue = row.label
        }

        [
                rowKey,
                row[column],
                probeValue,
                row.bioMarker,
                subsetName
        ]
    }

}
