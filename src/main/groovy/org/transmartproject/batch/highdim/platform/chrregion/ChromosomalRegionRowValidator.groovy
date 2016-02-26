package org.transmartproject.batch.highdim.platform.chrregion

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.transmartproject.batch.highdim.datastd.ChromosomalRegionValidator
import org.transmartproject.batch.highdim.datastd.PlatformOrganismValidator

/**
 * Validates {@link org.transmartproject.batch.highdim.platform.chrregion.ChromosomalRegionRow} objects.
 */
@Component
@JobScope
class ChromosomalRegionRowValidator implements ChromosomalRegionValidator, PlatformOrganismValidator {

    @Override
    boolean supports(Class<?> clazz) {
        clazz == ChromosomalRegionRow
    }

    @Override
    @SuppressWarnings('ReturnNullFromCatchBlock')
    void validate(Object target, Errors errors) {
        assert target instanceof ChromosomalRegionRow

        ChromosomalRegionValidator.super.validate(target, errors)
        PlatformOrganismValidator.super.validate(target, errors)

        if (!target.regionName) {
            errors.rejectValue 'regionName', 'required',
                    ['regionName'] as Object[], null
        }
    }
}
