/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.multidimquery

interface HypercubeValue {

    def getValue()

    def getAt(Dimension dim)

    /**
     * @param dim The dimension
     * @return The index of the element for this dimension in the list returned by Hypercube.dimensionElements(dim).
     * If this value does not have an element for this index, returns null.
     */
    Integer getDimElementIndex(Dimension dim)

    def getDimKey(Dimension dim)

    List<Dimension> getAvailableDimensions()

}