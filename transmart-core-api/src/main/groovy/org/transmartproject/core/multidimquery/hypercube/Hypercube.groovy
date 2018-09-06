package org.transmartproject.core.multidimquery.hypercube

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.SortSpecification

@CompileStatic
@Canonical
class Hypercube {
    List<DimensionProperty> dimensionDeclarations
    List<Cell> cells
    Map<String, List<Map<String, Object>>> dimensionElements
    List<SortSpecification> sort
}
