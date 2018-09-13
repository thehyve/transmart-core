package org.transmartproject.core.multidimquery.aggregates

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class AggregatesPerNumericalConcept {
    Map<String, NumericalValueAggregates> aggregatesPerNumericalConcept
}
