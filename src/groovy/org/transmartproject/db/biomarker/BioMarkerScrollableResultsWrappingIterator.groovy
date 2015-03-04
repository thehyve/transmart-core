package org.transmartproject.db.biomarker

import org.hibernate.ScrollableResults
import org.transmartproject.core.biomarker.BioMarker
import org.transmartproject.core.biomarker.BioMarkerResult
import org.transmartproject.db.util.SimpleScrollableResultsWrappingIterator

class BioMarkerScrollableResultsWrappingIterator extends SimpleScrollableResultsWrappingIterator<BioMarker> implements BioMarkerResult {

    BioMarkerScrollableResultsWrappingIterator(ScrollableResults scrollableResults) {
        super(scrollableResults)
    }
}
