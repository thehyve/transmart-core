package org.transmartproject.core.multidimquery.query

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import org.transmartproject.core.binding.BindingException
import org.transmartproject.core.binding.BindingHelper

import java.text.SimpleDateFormat

/**
 * A Constraint factory that creates {@link Constraint} objects from a JSON string using
 * the Jackson deserialiser.
 * Produces constraints for the classes:
 * - {@link TrueConstraint}
 * - {@link BiomarkerConstraint}
 * - {@link ModifierConstraint}
 * - {@link FieldConstraint}
 * - {@link ValueConstraint}
 * - {@link TimeConstraint}
 * - {@link PatientSetConstraint}
 * - {@link Negation}
 * - {@link Combination}
 * - {@link TemporalConstraint}
 * - {@link ConceptConstraint}
 * - {@link StudyNameConstraint}
 * - {@link NullConstraint}
 */
@CompileStatic
class ConstraintFactory {

    /**
     * Create a constraint object from a map of values
     * using Jackson and validates the constraint.
     *
     * *Deprecated*: Use {@link ConstraintFactory#read(String)} instead.
     *
     * @param constraintMap
     * @return a validated constraint
     */
    @Deprecated
    static Constraint create(Map constraintMap) {
        read(BindingHelper.objectMapper.writeValueAsString(constraintMap))
    }

    /**
     * Create a constraint object from a JSON string
     * using Jackson and validates the constraint.
     *
     * @param src the JSON string
     * @return a validated constraint
     */
    static Constraint read(String src) {
        try {
            Constraint constraint = BindingHelper.objectMapper.readValue(src, Constraint.class)
            BindingHelper.validate(constraint)
            constraint
        } catch (JsonProcessingException e) {
            throw new BindingException("Cannot parse constraint parameter: ${e.message}", e)
        }
    }

}
