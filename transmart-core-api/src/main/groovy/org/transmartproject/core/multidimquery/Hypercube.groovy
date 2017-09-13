/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.multidimquery

import org.transmartproject.core.IterableResult

interface Hypercube extends IterableResult<HypercubeValue> {

    Iterator<HypercubeValue> iterator()

    List<Object> dimensionElements(Dimension dim)

    List<Dimension> getDimensions()

    Object dimensionElement(Dimension dim, Integer idx)

    Object dimensionElementKey(Dimension dim, Integer idx)
}