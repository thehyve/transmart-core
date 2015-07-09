package org.transmartproject.batch.clinical.variable

import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator

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

        // accepted conceptTypes in the columnFile are:
        // null (ie empty string) or CATEGORICAL or NUMERICAL
        if (target.conceptType != null &&
                target.conceptType != 'CATEGORICAL' &&
                target.conceptType != 'NUMERICAL') {
            errors.rejectValue('conceptType', 'conceptTypeWrong',
                    [target.conceptType] as Object[], null)
        }
    }
}
