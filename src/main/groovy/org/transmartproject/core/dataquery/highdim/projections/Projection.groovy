package org.transmartproject.core.dataquery.highdim.projections

import org.transmartproject.core.dataquery.DataQueryResult

/**
 *
 * @param < CELL > The type of the cells in the {@link DataQueryResult}.
 */
public interface Projection<CELL> {

    CELL doWithResult(Object object)

}
