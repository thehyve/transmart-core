package org.transmartproject.batch.highdim.datastd

import org.springframework.validation.Errors
import org.springframework.validation.Validator

/**
 * Validates {@link ChromosomalRegionSupport} objects.
 */
trait ChromosomalRegionValidator implements Validator {

    public static final String VALID_CHR_REGEX = /[0-9]+|[XYM]/

    boolean optionalDefinition = true

    boolean supports(Class<?> clazz) {
        ChromosomalRegionSupport.isAssignableFrom(clazz)
    }

    @SuppressWarnings('ReturnNullFromCatchBlock')
    void validate(Object target, Errors errors) {
        assert target instanceof ChromosomalRegionSupport

        String chromosome = target.chromosome?.trim()
        if (optionalDefinition
                && !chromosome && target.startBp == null && target.endBp == null) {
            return
        }

        def chrErrArg = ['chromosome'] as Object[]

        if (!chromosome) {
            errors.rejectValue 'chromosome', 'required',
                    chrErrArg, null
        } else if (!(chromosome ==~ VALID_CHR_REGEX)) {
            errors.rejectValue 'chromosome', 'invalidChromosome',
                    chrErrArg, null
        }

        def startBpErrArg = ['startBp'] as Object[]

        boolean startBpIsValid = false
        if (target.startBp == null) {
            errors.rejectValue 'startBp', 'required',
                    startBpErrArg, null
        } else if (target.startBp < 0) {
            errors.rejectValue 'startBp', 'connotBeNegative',
                    startBpErrArg, null
        } else {
            startBpIsValid = true
        }

        if (target.endBp == null) {
            errors.rejectValue 'endBp', 'required',
                    ['endBp'] as Object[], null
        } else if (startBpIsValid && target.startBp > target.endBp) {
            errors.rejectValue 'endBp', 'invalidRange',
                    ['startBp', 'endBp'] as Object[], null
        }

    }
}
