/* (c) Copyright 2017, tranSMART Foundation, Inc. */

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
}