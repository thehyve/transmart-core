package org.transmartproject.batch.clinical.variable

import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator

import static org.transmartproject.batch.clinical.variable.ClinicalVariable.CONCEPT_TYPE_CATEGORICAL
import static org.transmartproject.batch.clinical.variable.ClinicalVariable.CONCEPT_TYPE_NUMERICAL

/**
 * Validates the 7th column of column mapping files
 */
@Component // no state; can be singleton
class ColumnMappingValidator implements Validator {

    @Override
    boolean supports(Class<?> clazz) {
        clazz == ClinicalVariable
    }

    @Override
    void validate(Object target, Errors errors) {
        assert target instanceof ClinicalVariable

        if (target.conceptType != null &&
                target.conceptType != CONCEPT_TYPE_CATEGORICAL &&
                target.conceptType != CONCEPT_TYPE_NUMERICAL) {
            errors.rejectValue('conceptType', 'conceptTypeWrong',
                    [target.conceptType] as Object[], null)
        }
    }
}
