package org.transmartproject.batch.i2b2.mapping

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator

/**
 * ... validates {@link I2b2WordMapping}s.
 */
@Component
@Scope('singleton')
class I2b2WordMappingValidator implements Validator {

    @Override
    boolean supports(Class<?> clazz) {
        clazz == I2b2WordMapping
    }

    @Override
    void validate(Object target, Errors errors) {
        I2b2WordMapping wordMapping = target

        ['filename', 'columnNumber'].each {
            if (!wordMapping."$it") {
                errors.rejectValue(it, 'required', [it] as Object[], null)
            }
        }

        if (wordMapping.columnNumber && wordMapping.columnNumber < 1) {
            errors.rejectValue('columnNumber', 'mustBePositive',
                    [] as Object[], null)
        }

        // from and to can be empty
    }
}
