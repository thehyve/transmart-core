package org.transmartproject.core.dataquery.acgh

import org.transmartproject.core.dataquery.assay.Assay

/**
 * A <code>RegionRow</code> associates a list of {@link Assay}s with {@link
 * ACGHValues} (one for each assay) in the context of an aCGH region.
 */
public interface RegionRow {

    /**
     * The region for which this row provides {@link ACGHValues} objects.
     *
     * @return the region to which this {@link RegionRow} refers
     */
    Region getRegion()

    /**
     * The set of the values for the assay with the specified index.
     * The index -> map is the same as the one specified by the list returned
     * by {@link RegionResult#getIndicesList()}.
     *
     * @param index a 0-based index, upper bounded by the number of assays -1
     * @return the values object for the (region, assay) pair
     */
    ACGHValues getAt(int index) throws ArrayIndexOutOfBoundsException

    /**
     * The set of values for the passed assay.
     *
     * @param assay An assay valid for the result
     * @return the values object for the (region, assay) pair
     */
    ACGHValues getRegionDataForAssay(Assay assay) throws ArrayIndexOutOfBoundsException
}
