package representations

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class NumericalValueAggregates {
    Double min
    Double max
    Double avg
    Integer count
    Double stdDev
}

@EqualsAndHashCode
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

@EqualsAndHashCode
class Aggregates {
    CategoricalValueAggregates categoricalValueAggregates
    NumericalValueAggregates numericalValueAggregates
}

@EqualsAndHashCode
class AggregatesPerConcept {

    /**
     * Map from concept code to aggregates.
     */
    Map<String, Aggregates> aggregatesPerConcept = [:]

}
