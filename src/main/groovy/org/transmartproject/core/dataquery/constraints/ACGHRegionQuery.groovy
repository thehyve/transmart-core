package org.transmartproject.core.dataquery.constraints

import org.transmartproject.core.dataquery.acgh.ChromosomalSegment
import org.transmartproject.core.dataquery.acgh.Region

class ACGHRegionQuery {

    /**
     * Constraints that are common to all high dimensional data.
     */
    CommonHighDimensionalQueryConstraints common

    /**
     * If not null, limit the query to the given set of regions.
     */
    Set<Region> regions

    Set<ChromosomalSegment> segments

}
