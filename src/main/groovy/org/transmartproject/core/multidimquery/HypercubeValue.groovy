package org.transmartproject.core.multidimquery

import com.google.common.collect.ImmutableSet

interface HypercubeValue {

    def getValue()

    def getAt(Dimension dim)

    int getDimElementIndex(Dimension dim)

    def getDimKey(Dimension dim)

    ImmutableSet<Dimension> availableDimensions()

}