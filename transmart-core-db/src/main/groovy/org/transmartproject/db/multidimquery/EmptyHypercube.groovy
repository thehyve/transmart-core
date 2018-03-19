/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.multidimquery

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import org.transmartproject.core.dataquery.SortOrder
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue

class EmptyHypercube implements Hypercube {

    ImmutableList<Object> dimensionElements(Dimension dim) {
        throw new InvalidArgumentsException("Dimension $dim is not part of this result")
    }

    ImmutableList<Dimension> getDimensions() { ImmutableList.of() }

    Object dimensionElement(Dimension dim, Integer idx) {
        throw new InvalidArgumentsException("Dimension $dim is not part of this result")
    }

    Object dimensionElementKey(Dimension dim, Integer idx) {
        throw new InvalidArgumentsException("Dimension $dim is not part of this result")
    }

    void close() {}

    Iterator<HypercubeValue> iterator() {
        [].iterator()
    }

    ImmutableMap<Dimension, SortOrder> getSortOrder() { ImmutableMap.of() }
}
