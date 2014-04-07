package org.transmartproject.core.dataquery.highdim.projections


public interface MultiValueProjection {

    /**
     * Returns a set with the keys/properties that are available in the object returned by
     * queries using this projection.
     */
    public Collection<String> getDataProperties()


}
