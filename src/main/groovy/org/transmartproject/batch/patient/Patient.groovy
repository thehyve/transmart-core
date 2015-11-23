package org.transmartproject.batch.patient

import groovy.transform.ToString
import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import org.transmartproject.batch.concept.ConceptType

import javax.batch.operations.BatchRuntimeException

/**
 * Unsurprisingly, represents a patient.
 */
@ToString(includes = ['id', 'code'])
@Slf4j
@TypeChecked
final class Patient {
    String id
    Long code

    private final Map<DemographicVariable, Object> demographicValues = [:]

    void putDemographicValues(Map<DemographicVariable, String> values) {
        values.each { DemographicVariable var, Object value ->
            if (demographicValues.containsKey(var)) {
                log.warn "For patient $id, and demo variable $var, " +
                        "replacing ${demographicValues[var]} with $value"
            }

            try {
                demographicValues[var] = var.type == ConceptType.NUMERICAL ?
                        (value ?: null) as Long :
                        value
            } catch (NumberFormatException nfe) {
                throw new BatchRuntimeException(
                        "Value $value for variable $var in patient $this is " +
                                "not a valid long integer", nfe)
            }
        }
    }

    Object getDemographicValue(DemographicVariable var) {
        demographicValues[var]
    }

    boolean isNew() {
        code == null
    }

}
