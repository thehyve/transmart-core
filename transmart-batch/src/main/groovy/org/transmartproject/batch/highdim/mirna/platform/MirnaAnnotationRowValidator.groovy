package org.transmartproject.batch.highdim.mirna.platform

import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator

/**
 * Validates {@link MirnaAnnotationRow} objects.
 */
@Component
@JobScope
@Slf4j
class MirnaAnnotationRowValidator implements Validator {

    @Override
    boolean supports(Class<?> clazz) {
        clazz == MirnaAnnotationRow
    }

    @Override
    @SuppressWarnings('ReturnNullFromCatchBlock')
    void validate(Object target, Errors errors) {
        assert target instanceof MirnaAnnotationRow

        if (!target.idRef) {
            errors.rejectValue 'idRef', 'required',
                    ['idRef'] as Object[], null
        }
        if (!target.mirnaId) {
            errors.rejectValue 'mirnaId', 'required',
                    ['mirnaId'] as Object[], null
        }
    }
}
