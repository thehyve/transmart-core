/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.db.multidimquery

import com.google.common.collect.ImmutableList
import com.google.common.collect.PeekingIterator
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue

class EmptyHypercube implements Hypercube {

    final boolean DimensionsPreloadable = false
    final boolean DimensionsPreloaded =  false
    final boolean AutoloadDimensions = false

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

    PeekingIterator<HypercubeValue> iterator() {
        return new PeekingIterator() {
            @Override boolean hasNext() { return false }
            @Override Object next() { throw new NoSuchElementException() }
            @Override Object peek() { throw new NoSuchElementException() }
            @Override void remove() { throw new UnsupportedOperationException() }
        }
    }


}
