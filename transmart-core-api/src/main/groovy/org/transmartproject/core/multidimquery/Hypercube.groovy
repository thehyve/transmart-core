/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.multidimquery

import com.google.common.collect.ImmutableMap
import org.transmartproject.core.IterableResult
import org.transmartproject.core.dataquery.SortOrder

/**
 * The Hypercube represents data as a multidimensional data cube. The primary interface is the iterator(), which
 * iterates over cells in the multidimensional cube. The cube can be (and often is) sparse. Each cell, represented as
 * a HypercubeValue, knows its own value and its co-ordinates in the (potentially many) dimensions of this result.
 * There can also be multiple values for the same cell, for example when the data set has a dimension that is not
 * included in the query that returned the hypercube. In such a case values that differ in the excluded dimension
 * only would be squashed in the resulting view. The iterator will then return multiple HypercubeValue's that have
 * the same co-ordinates in all dimensions that are part of this cube.
 *
 * For representing the dimensions, the Hypercube distinguishes dense and sparse dimensions. The basic difference is
 * that for a dense dimension one would expect many HypercubeValues that share the same co-ordinate (element) in this
 * dimension, while for a sparse dimension one would expect very few HypercubeValues to share the same element for
 * that dimension, or for each HypercubeValue to have a unique co-ordinate/element in that dimension.
 *
 * For example, the patient and concept dimensions are dense. There are often many values associated to a single
 * patient and also many values associated to a single concept. An example of a sparse dimension is 'start time'. If
 * individual values are entered with a high resolution timestamp, the start time would be different for almost every
 * value. Another such example might be a modifier dimension that represents the dosage of a certain medicine that a
 * patient received.
 *
 * The main reason for differentiating between dense and sparse dimensions is to avoid serializing elements of dense
 * dimensions many times. For example, a naive serialization of a query result could be a list of serialized
 * HypercubeValues with the dimension elements inlined. Then the data for a patient would be included with every
 * observation for that patient, and this would result in a large space overhead. Therefore, another concern in
 * deciding if a dimension will be modeled as dense or sparse is the size of the serialized elements.
 */
interface Hypercube extends IterableResult<HypercubeValue> {

    /**
     * @return an iterator with the actual values as HypercubeValue's
     */
    Iterator<HypercubeValue> iterator()

    /**
     * @param dim the dimension
     * @return a list of elements for this dimension.
     */
    List<Object> dimensionElements(Dimension dim)

    /**
     * @return The list of dimensions that are part of this result. This is a subset of the dimensions that are part
     * of the queried studies.
     */
    List<Dimension> getDimensions()

    Object dimensionElement(Dimension dim, Integer idx)

    /* TODO: this should probably not be public, most users should use the dimension elements themselves or the
     * indexes
     * TODO 2: calling this method should not force-load all dimensions, doing that should be a separate call.
     */
    Object dimensionElementKey(Dimension dim, Integer idx)

    /**
     * An ordered map of Dimension to SortOrder specifying the sort order that was used in this result. The sort
     * order can include sorting on dimensions that was not requested if that was needed due to implementation concerns.
     * @return the sort order as an ordered map
     */
    ImmutableMap<Dimension, SortOrder> getSortOrder()
}
