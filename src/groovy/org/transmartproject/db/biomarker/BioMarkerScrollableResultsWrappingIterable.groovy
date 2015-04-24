package org.transmartproject.db.biomarker

import org.hibernate.ScrollableResults
import org.transmartproject.core.biomarker.BioMarker
import org.transmartproject.core.biomarker.BioMarkerResult
import org.transmartproject.db.util.SimpleScrollableResultsWrappingIterable

class BioMarkerScrollableResultsWrappingIterable extends SimpleScrollableResultsWrappingIterable<BioMarker> implements BioMarkerResult {

    BioMarkerScrollableResultsWrappingIterable(ScrollableResults scrollableResults) {
        super(scrollableResults)
    }
}
