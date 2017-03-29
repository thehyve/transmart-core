/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.core.multidimquery

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet


/**
 * A HypercubeValue represents a single cell in the N-dimensional cube represented by {@Link Hypercube}. For each
 * HypercubeValue you can retrieve the value itself, and the dimension 'coordinates' for this cell in the form of
 * dimension elements.
 */
interface HypercubeValue {

    /**
     * @return The value of this cell. In the current Transmart implementation this can be a String or a
     * Double/BigDecimal. The Hypercube format in principle supports any type of values but this is not used.
     * The Hypercube api also supports null values, but that too is not used in Transmart as of version 17.
     */
    def getValue()

    /**
     * @param dim The dimension
     * @return The dimension element for the specified dimension for the current cell
     */
    def getAt(Dimension dim)

    /**
     * @param dim The dimension, which must not be sparse
     * @return The index of the element for this dimension in the list returned by Hypercube.dimensionElements(dim).
     * If this value does not have an element for this index, returns null.
     *
     * Indexes can be used to identify dimension elements in an efficient way, similar to dimension element keys.
     * Indexes are only available for dimensions that are dense
     *
     * @see Dimension#getDensity
     */
    Integer getDimElementIndex(Dimension dim)

    /**
     * Equivalent to {@Link Hypercube#dimensionElementKey} for the current value, but this also works for sparse
     * dimensions. The key can be a String, numeric value, or Date and uniquely identifies the dimension element. For
     * sparse dimensions, the key is often the same as the dimension element itself.
     *
     * @param dim the dimension
     * @return the element's key
     * @see Dimension#getElementsSerializable()
     */
    def getDimKey(Dimension dim)

    /**
     * @return the list of dimensions that are applicable to this hypercube and thus this HypercubeValue.
     */
    ImmutableList<Dimension> getAvailableDimensions()
}