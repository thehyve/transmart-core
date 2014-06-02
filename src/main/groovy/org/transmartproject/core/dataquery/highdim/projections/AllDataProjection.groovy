package org.transmartproject.core.dataquery.highdim.projections


/**
 * Created by jan on 1/27/14.
 */
public interface AllDataProjection extends Projection<Map<String, Object>>, MultiValueProjection {

    /**
     * Returns a map with the names and types of the datatype specific properties that
     * are available on the rows belonging to this datatype.
     */
    public Map<String, Class> getRowProperties()

}
