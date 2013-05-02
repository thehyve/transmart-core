package org.transmartproject.core.dataquery

import org.transmartproject.core.dataquery.constraints.ACGHRegionQuery
import org.transmartproject.core.dataquery.acgh.RegionResult

/**
 * A resource for querying about clinical and high-dimensional data.
 */
public interface DataQueryResource {

    RegionResult runACGHRegionQuery(ACGHRegionQuery spec)
}
