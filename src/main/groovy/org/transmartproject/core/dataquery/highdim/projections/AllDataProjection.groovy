package org.transmartproject.core.dataquery.highdim.projections


/**
 * Created by jan on 1/27/14.
 */
public interface AllDataProjection extends Projection<Map<String, Object>>{

    /**
     * Returns a set with the names of the datatype specific properties that
     * are available on the rows belonging to this datatype.
     */
    public Collection<String> getRowProperties()

    /**
     * Returns a set with the keys that are available in the map returned by
     * queries using this projection.
     */
    public Collection<String> getDataProperties()

}
