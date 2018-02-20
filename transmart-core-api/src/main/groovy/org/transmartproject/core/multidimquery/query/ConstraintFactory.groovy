package org.transmartproject.core.multidimquery.query

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import java.text.DateFormat
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

    private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator()
    private static final ObjectMapper objectMappper = new ObjectMapper().setDateFormat(DATE_TIME_FORMAT)

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
        read(objectMappper.writeValueAsString(constraintMap))
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
            Constraint constraint = objectMappper.readValue(src, Constraint.class)
            validate(constraint)
            constraint
        } catch (JsonProcessingException e) {
            throw new ConstraintBindingException("Cannot parse constraint parameter: ${e.message}", e)
        }
    }

    static validate(Constraint constraint) {
        Set<ConstraintViolation<Constraint>> errors = validator.validate(constraint)
        if (errors) {
            String sErrors = errors.collect { "${it.propertyPath.toString()}: ${it.message}" }.join('; ')
            throw new ConstraintBindingException("${errors.size()} error(s): ${sErrors}", errors)
        }
    }

}
