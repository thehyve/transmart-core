package org.transmartproject.core.multidimquery

import com.google.common.collect.ImmutableList
import com.google.common.collect.PeekingIterator
import org.transmartproject.core.IterableResult

interface Hypercube extends IterableResult<HypercubeValue> {

    PeekingIterator<HypercubeValue> iterator()

    ImmutableList<Object> dimensionElements(Dimension dim)

    ImmutableList<Dimension> getDimensions()

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
}