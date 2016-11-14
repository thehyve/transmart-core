package org.transmartproject.core.multidimquery

import com.google.common.collect.ImmutableList
import org.transmartproject.core.IterableResult

interface Hypercube extends IterableResult<HypercubeValue> {

    ImmutableList<Object> dimensionElements(Dimension dim)

    ImmutableList<Dimension> getDimensions()

    Object dimensionElement(Dimension dim, Integer idx)

    Object dimensionElementKey(Dimension dim, Integer idx)

    void loadDimensions()

    boolean getDimensionsLoaded()


}