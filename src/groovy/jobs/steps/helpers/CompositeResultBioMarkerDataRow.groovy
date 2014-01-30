package jobs.steps.helpers

import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow

class CompositeResultBioMarkerDataRow extends CompositeResultDataRow implements BioMarkerDataRow<Number> {

    BioMarkerDataRow getDelegateRow() {
        this.delegateRow
    }

    @Override
    String getBioMarker() {
        delegateRow.bioMarker
    }
}
