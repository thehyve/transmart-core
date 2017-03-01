/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.db.multidimquery

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.PeekingIterator
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.core.multidimquery.IndexGetter

class EmptyHypercube implements Hypercube {

    final boolean DimensionsPreloadable = false
    final boolean DimensionsPreloaded =  false
    final boolean AutoloadDimensions = false
    final ImmutableMap sorting = ImmutableMap.of()

    void loadDimensions() {}
    ImmutableList<Object> dimensionElements(Dimension dim){
        throw new InvalidArgumentsException("Dimension $dim is not part of this result")
    }

    ImmutableList<Dimension> getDimensions() { ImmutableList.of() }

    Object dimensionElement(Dimension dim, Integer idx) {
        throw new InvalidArgumentsException("Dimension $dim is not part of this result")
    }

    Object dimensionElementKey(Dimension dim, Integer idx) {
        throw new InvalidArgumentsException("Dimension $dim is not part of this result")
    }

    void close(){}

    void setAutoloadDimensions(boolean value){}

    void preloadDimensions(){}

    // I think EmptyHypercube is sometimes returned when there are no results but there might be valid dimensions. So
    // we will assume that all dimensions are valid and only throw if the IndexGetter is invoked
    IndexGetter getIndexGetter(Dimension dimension) {
        { v -> throw new IllegalArgumentException(
                "HypercubeValue $v does not belong to this (or any) EmptyHypercube") } as IndexGetter
    }

    PeekingIterator<HypercubeValue> iterator() {
        return new PeekingIterator() {
            @Override boolean hasNext() { return false }
            @Override Object next() { throw new NoSuchElementException() }
            @Override Object peek() { throw new NoSuchElementException() }
            @Override void remove() { throw new UnsupportedOperationException() }
        }
    }


}
