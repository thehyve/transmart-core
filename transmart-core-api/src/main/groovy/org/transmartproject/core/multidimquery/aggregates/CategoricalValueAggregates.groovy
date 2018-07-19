package org.transmartproject.core.multidimquery.aggregates

import groovy.transform.Canonical

/**
 * Categorical value aggregates
 */
@Canonical
class CategoricalValueAggregates {
    /**
     * Keys are values and values are counts. e.g. {Female: 345, Male 321}
     */
    Map<String, Integer> valueCounts

    /**
     * Counts for values equal null.
     */
    Integer nullValueCounts
}
