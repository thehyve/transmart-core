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

    @Resource
    Platform platformObject

    boolean supports(Class<?> clazz) {
        clazz == ProteomicsAnnotationRow
    }

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

    }
}
