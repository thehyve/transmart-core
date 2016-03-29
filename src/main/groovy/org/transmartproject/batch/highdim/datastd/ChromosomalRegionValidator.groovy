package org.transmartproject.batch.highdim.datastd

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.transmartproject.batch.highdim.platform.Platform

import javax.annotation.Resource

/**
 * Validates {@link ChromosomalRegionSupport} objects.
 */
@Component
@JobScope
class ChromosomalRegionValidator implements Validator {

    public static final String VALID_CHR_REGEX = /[0-9]+|[XYM]/

    boolean optionalDefinition = true

    @Resource
    Platform platformObject

    boolean supports(Class<?> clazz) {
        ChromosomalRegionSupport.isAssignableFrom(clazz)
    }

    void validate(Object target, Errors errors) {
        assert target instanceof ChromosomalRegionSupport

        String chromosome = target.chromosome?.trim()
        def chrInfoSpecified = chromosome || target.startBp != null || target.endBp != null
        if (optionalDefinition && !chrInfoSpecified) {
            return
        }

        if (chrInfoSpecified && !platformObject.genomeRelease) {
            errors.reject('genReleaseMandatoryIfChrInfoSpecified')
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
