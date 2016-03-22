package org.transmartproject.batch.highdim.datastd

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.transmartproject.batch.highdim.platform.Platform

import javax.annotation.Resource

/**
 * Validates {@link PlatformOrganismSupport} objects.
 */
@Component
@JobScope
class PlatformValidator implements Validator {

    @Resource
    Platform platformObject

    boolean supports(Class<?> clazz) {
        PlatformOrganismSupport.isAssignableFrom(clazz)
    }

    void validate(Object target, Errors errors) {
        assert target instanceof PlatformOrganismSupport

        /* platformObject.id should be normalized to uppercase because it comes
           * from the normalized parameter PLATFORM, but it may be that platform
           * name in the data file is not */
        if (target.gplId.toUpperCase(LocaleContextHolder.locale) != platformObject.id) {
            errors.rejectValue 'gplId', 'expectedConstant',
                    [platformObject.id, target.gplId] as Object[], null
        }

        if (!platformObject.organism.equalsIgnoreCase(target.organism)) {
            errors.rejectValue 'organism', 'expectedConstant',
                    [platformObject.organism, target.organism] as Object[],
                    null
        }
    }
}
