/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.core.multidimquery

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.PeekingIterator
import org.transmartproject.core.IterableResult
import org.transmartproject.core.multidimquery.dimensions.Order

interface Hypercube extends IterableResult<HypercubeValue> {

    PeekingIterator<HypercubeValue> iterator()

    ImmutableList<Object> dimensionElements(Dimension dim)

    ImmutableList<Dimension> getDimensions()

    /**
     * An ordered map that includes the dimensions on which the result is ordered. The order in the map is the same
     * as the order in which the sorting is applied to the result. The values indicate if the sorting is ascending or
     * descending.
     */
    ImmutableMap<Dimension, Order> getSorting()

    Object dimensionElement(Dimension dim, Integer idx)

    Object dimensionElementKey(Dimension dim, Integer idx)

    /**
     * Returns true if dimensions can be preloaded.
     */
    boolean isDimensionsPreloadable()

    /**
     * Load all dimensions used in this dataset. If dimensionsPreloadable == false this method throws
     * UnsupportedOperationException
     */
    void preloadDimensions()

    boolean isDimensionsPreloaded()

    /**
     * If true (default), dimensions will be automatically loaded once all values are exhausted. For some
     * implementations this property does not do anything.
     */
    boolean isAutoloadDimensions()
    void setAutoloadDimensions(boolean autoload)

    /**
     * Loads all dimension elements for the currently known dimension keys
     */
    void loadDimensions()

    /**
     * @return an IndexGetter that will (efficiently) retrieve the element index for the specified dimension. The
     * IndexGetter can only be used on HypercubeValues that were retrieved from this Hypercube. IndexGetters can be
     * used to efficiently compare HypercubeValue's for equality on selected dimensions.
     * @param dim The dimension for which to retrieve the index
     */
    IndexGetter getIndexGetter(Dimension dim)
}

interface IndexGetter {
    /**
     * Retrieve the element index for the dimension that was passed to the factory function when this IndexGetter was
     * created
     * @param value the HypercubeValue
     * @return the index, or null if not present for this value
     */
    Integer call(HypercubeValue value)
}
