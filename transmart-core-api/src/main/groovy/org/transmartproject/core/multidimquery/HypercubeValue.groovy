/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.multidimquery

import org.transmartproject.core.multidimquery.hypercube.Dimension

/**
 * A HypercubeValue models a single cell in a multidimensional data cube. Each cell has co-ordinates in each
 * dimension that is part of the Hypercube to which this value belongs. A HypercubeValue also has a single value.
 * There can be multiple values in the same cell of a hypercube, in that case those values are represented as
 * separate HypercubeValues that have the same set of co-ordinates.
 */
interface HypercubeValue {

    /**
     * @return the value
     */
    def getValue()

    /**
     * @param dim the dimension
     * @return the co-ordinate of this value cell with respect to the specified dimension. This co-ordinate is one of
     * the elements that makes up the dimension.
     */
    def getAt(Dimension dim)

    /**
     * For dense dimensions, the dimension elements are stored in an indexed list, to avoid duplicates in
     * serialization of the result set. {@link #getAt} will return the dimension element for any type of dimension,
     * for dense dimensions this method returns the element's index.
     *
     * Indexes are unique to a dimension element only within one hypercube and within that dimension. Indexes are
     * guaranteed to be contiguous, starting from 0.
     *
     * @param dim The dimension
     * @return The index of the dimension element for the specified dimension. If this HypercubeValue does not have
     * an element for this dimension, returns null.
     * @throws UnsupportedOperationException if dim is not a {@link Dimension.Density#DENSE} dimension
     * @throws IllegalArgumentException if dim is not part of this Hypercube
     */
    Integer getDimElementIndex(Dimension dim)

    /**
     * Get the dimension element's key. Each dimension  element has a key that uniquely identifies it. This key is
     * always a 'simple' object, i.e. a number, String, or Date.
     *
     * TODO: Do we need this method?
     *
     * @param dim the dimension
     * @return the dimension element's key value
     * @throws IllegalArgumentException if dim is not part of this Hypercube
     */
    def getDimKey(Dimension dim)

    /**
     * @return the list of dimensions that are part of this hypercube. This is the same as {@link
     * Hypercube#getDimensions}.
     */
    List<Dimension> getAvailableDimensions()

}
