package org.transmartproject.batch.patient

import groovy.transform.ToString
import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import org.springframework.batch.core.step.FatalStepExecutionException

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
                demographicValues[var] = var.name() == 'AGE' ?
                        value ? Math.floor(value as Float) as Integer : null :
                        value
            } catch (NumberFormatException nfe) {
                throw new FatalStepExecutionException(
                        "Value $value for variable $var in patient $this is " +
                                "not a valid number", nfe)
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
