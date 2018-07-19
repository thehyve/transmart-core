package org.transmartproject.core.multidimquery.hypercube

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class Cell {
    List<Object> inlineDimensions
    List<Integer> dimensionIndexes
    String stringValue
    Number numericValue
}
