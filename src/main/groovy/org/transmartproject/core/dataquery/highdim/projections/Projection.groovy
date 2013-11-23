package org.transmartproject.core.dataquery.highdim.projections

/**
 *
 * @param < CELL > The type of the cells in the {@link DataQueryResult}.
 */
public interface Projection<CELL> {

    CELL doWithResult(Object object)

}
