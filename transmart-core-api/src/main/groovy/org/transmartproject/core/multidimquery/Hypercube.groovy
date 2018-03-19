/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.multidimquery

import com.google.common.collect.ImmutableMap
import org.transmartproject.core.IterableResult
import org.transmartproject.core.dataquery.SortOrder

interface Hypercube extends IterableResult<HypercubeValue> {

    Iterator<HypercubeValue> iterator()

    List<Object> dimensionElements(Dimension dim)

    List<Dimension> getDimensions()

    Object dimensionElement(Dimension dim, Integer idx)

    Object dimensionElementKey(Dimension dim, Integer idx)

    /**
     * An ordered map of Dimension to SortOrder specifying the sort order that was used in this result. The sort
     * order can include sorting on dimensions that was not requested if that was needed due to implementation concerns.
     * @return the sort order as an ordered map
     */
    ImmutableMap<Dimension, SortOrder> getSortOrder()
}