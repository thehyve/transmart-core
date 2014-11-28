package org.transmartproject.batch.patient

import groovy.transform.ToString
import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import org.transmartproject.batch.concept.ConceptType

/**
 * Unsurprisingly, represents a patient.
 */
@ToString(includes=['id','code'])
@Slf4j
@TypeChecked
final class Patient {
    String id
    Long code

    // TODO: doesn't belong here (arguably)
    boolean isNew = true //new by default

    private final Map<DemographicVariable, Object> demographicValues = [:]

    void putDemographicValues(Map<DemographicVariable, String> values) {
        values.each { DemographicVariable var, String value ->
            if (demographicValues.containsKey(var)) {
                log.warn "For patient $id, and demo variable $var, " +
                        "replacing ${demographicValues[var]} with $value"
            }
            demographicValues[var] =  var.type == ConceptType.NUMERICAL ?
                    value as Long :
                    value
        }
    }

    Object getDemographicValue(DemographicVariable var) {
        demographicValues[var] ?: var.defaultValue
    }

}
