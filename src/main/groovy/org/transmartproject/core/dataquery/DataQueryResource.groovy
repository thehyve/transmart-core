package org.transmartproject.core.dataquery

import org.transmartproject.core.dataquery.constraints.ACGHRegionQuery
import org.transmartproject.core.dataquery.acgh.RegionResult

/**
 * A resource for querying about clinical and high-dimensional data.
 */
public interface DataQueryResource {

    /**
     * Runs a query for aCHG region data. Optionally,  it can be run on a
	 * separate session; a stateless session may be useful here.
     *
     * @param spec the specification of the query
     * @param session session where to run the query or null to use current
     * @return the query result result
     */
    RegionResult runACGHRegionQuery(ACGHRegionQuery spec, session)
}
