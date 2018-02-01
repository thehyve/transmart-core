package org.transmartproject.core.multidimquery

import groovy.transform.Immutable

/**
 * Numerical values aggregates
 */
@Immutable
class NumericalValueAggregates {
    Double min
    Double max
    Double avg
    Integer count
    Double stdDev
}
