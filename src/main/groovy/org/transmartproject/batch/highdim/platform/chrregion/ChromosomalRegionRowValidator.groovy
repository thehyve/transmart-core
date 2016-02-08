package org.transmartproject.batch.highdim.platform.chrregion

import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.transmartproject.batch.highdim.platform.Platform

import javax.annotation.Resource

import static org.springframework.context.i18n.LocaleContextHolder.locale

/**
 * Validates {@link org.transmartproject.batch.highdim.platform.chrregion.ChromosomalRegionRow} objects.
 */
@Component
@JobScope
@Slf4j
class ChromosomalRegionRowValidator implements Validator {

    public static final String VALID_CHR_REGEX = /[0-9]+|[XYM]/

    @Resource
    Platform platformObject

    @Override
    boolean supports(Class<?> clazz) {
        clazz == ChromosomalRegionRow
    }

    @Override
    @SuppressWarnings('ReturnNullFromCatchBlock')
    void validate(Object target, Errors errors) {
        assert target instanceof ChromosomalRegionRow

        /* platformObject.id should be normalized to uppercase because it comes
         * from the normalized parameter PLATFORM, but it may be that platform
         * name in the data file is not */
        if (target.gplId.toUpperCase(locale) != platformObject.id) {
            errors.rejectValue 'gplId', 'expectedConstant',
                    [platformObject.id, target.gplId] as Object[], null
        }

        if (!platformObject.organism.equalsIgnoreCase(target.organism)) {
            errors.rejectValue 'organism', 'expectedConstant',
                    [platformObject.organism, target.organism] as Object[],
                    null
        }

        //TODO Duplicated code
        //if entry has any chromosomal position information
        if (target.chromosome || target.startBp || target.endBp) {

            def chrErrArg = ['chromosome'] as Object[]

            if (!target.chromosome) {
                errors.rejectValue 'chromosome', 'required',
                        chrErrArg, null
            }

            if (!(target.chromosome ==~ VALID_CHR_REGEX)) {
                errors.rejectValue 'chromosome', 'invalidChromosome',
                        chrErrArg, null
            }

            def startBpErrArg = ['startBp'] as Object[]

            if (!target.startBp) {
                errors.rejectValue 'startBp', 'required',
                        startBpErrArg, null
            }

            if (target.startBp < 0) {
                errors.rejectValue 'startBp', 'connotBeNegative',
                        startBpErrArg, null
            }

            if (!target.endBp) {
                errors.rejectValue 'endBp', 'required',
                        ['endBp'] as Object[], null
            }

            if (target.startBp > target.endBp) {
                errors.rejectValue 'endBp', 'invalidRange',
                        ['startBp', 'endBp'] as Object[], null
            }

        }
    }
}
