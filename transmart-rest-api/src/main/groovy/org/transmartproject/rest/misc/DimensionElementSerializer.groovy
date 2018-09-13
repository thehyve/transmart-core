package org.transmartproject.rest.misc

import groovy.transform.Canonical
import org.transmartproject.core.multidimquery.hypercube.Dimension

@Canonical
class DimensionElementSerializer implements Iterator {
    Dimension dimension
    Iterator iterator

    boolean hasNext() { iterator.hasNext() }

    def next() {
        dimension.asSerializable(iterator.next())
    }
}
