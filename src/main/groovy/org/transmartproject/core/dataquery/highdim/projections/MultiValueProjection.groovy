package org.transmartproject.core.dataquery.highdim.projections


public interface MultiValueProjection {

    /**
     * Returns a set with the names of the datatype specific properties that
     * are available on the rows belonging to this datatype.
     */
    public Collection<String> getRowProperties()

}
