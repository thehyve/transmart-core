package org.transmartproject.db.dataquery.highdim

import org.transmartproject.core.dataquery.highdim.Platform

class PlatformImpl implements Platform {

    String  id
    String  title
    String  organism
    Date    annotationDate
    String  markerType
    String  genomeReleaseId

    @Override
    Iterable<?> getTemplate() {
        throw new UnsupportedOperationException()
    }
}
