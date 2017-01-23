package org.transmartproject.batch.highdim.rnaseq.data

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator

/**
 * Validates {@link org.transmartproject.batch.highdim.rnaseq.data.RnaSeqDataValue} objects.
 */
@Component
@Slf4j
class RnaSeqDataValueValidator implements Validator {

    @Override
    boolean supports(Class<?> clazz) {
        clazz == RnaSeqDataValue
    }

    @Override
    @SuppressWarnings('ReturnNullFromCatchBlock')
    void validate(Object target, Errors errors) {
        RnaSeqDataValue item = target

        if (!item.sampleCode) {
            errors.rejectValue 'sampleCode', 'required',
                    ['sample'] as Object[], null
        }
        if (!item.annotation) {
            errors.rejectValue 'annotation', 'required',
                    ['gene'] as Object[], null
        }
        if (item.readCount == null) {
            errors.rejectValue 'readCount', 'required',
                    ['readCount'] as Object[], null
        }
    }
}
