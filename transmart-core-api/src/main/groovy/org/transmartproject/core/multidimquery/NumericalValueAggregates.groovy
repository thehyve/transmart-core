package org.transmartproject.core.multidimquery

import groovy.transform.Canonical
import groovy.transform.Immutable

/**
 * Numerical values aggregates
 */
@Canonical
class NumericalValueAggregates {
    Double min
    Double max
    Double avg
    Integer count
    Double stdDev
}
