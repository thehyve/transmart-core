package org.transmartproject.core.dataquery.highdim.projections


/**
 * Created by jan on 1/27/14.
 */
public interface AllDataProjection extends Projection<Map<String, Object>>, MultiValueProjection {

    /**
     * Returns a set with the keys that are available in the map returned by
     * queries using this projection.
     */
    public Collection<String> getDataProperties()

}
