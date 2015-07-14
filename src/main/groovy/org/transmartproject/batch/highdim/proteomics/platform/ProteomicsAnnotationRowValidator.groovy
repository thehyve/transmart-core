package org.transmartproject.batch.highdim.proteomics.platform

import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.transmartproject.batch.highdim.platform.Platform

import javax.annotation.Resource

import static org.springframework.context.i18n.LocaleContextHolder.locale

/**
 * Validates {@link org.transmartproject.batch.highdim.proteomics.platform.ProteomicsAnnotationRow} objects.
 */
@Component
@JobScope
@Slf4j
class ProteomicsAnnotationRowValidator implements Validator {

    public static final String VALID_CHR_REGEX = /[0-9]+|[XYM]/

    @Resource
    Platform platformObject

    @Override
    boolean supports(Class<?> clazz) {
        clazz == ProteomicsAnnotationRow
    }

    @Override
    @SuppressWarnings('ReturnNullFromCatchBlock')
    void validate(Object target, Errors errors) {
        assert target instanceof ProteomicsAnnotationRow

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

        if (!target.probesetId) {
            errors.rejectValue 'probesetId', 'required',
                    ['probesetId'] as Object[], null
        }

        if (!target.uniprotId) {
            errors.rejectValue 'uniprotId', 'required',
                    ['uniprotId'] as Object[], null
        }

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
