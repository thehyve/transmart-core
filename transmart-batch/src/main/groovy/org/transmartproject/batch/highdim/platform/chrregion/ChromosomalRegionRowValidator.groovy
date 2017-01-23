package org.transmartproject.batch.highdim.platform.chrregion

import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator

/**
 * Validates {@link org.transmartproject.batch.highdim.platform.chrregion.ChromosomalRegionRow} objects.
 */
@Component
class ChromosomalRegionRowValidator implements Validator {

    @Override
    boolean supports(Class<?> clazz) {
        clazz == ChromosomalRegionRow
    }

    @Override
    void validate(Object target, Errors errors) {
        assert target instanceof ChromosomalRegionRow

        if (!target.regionName) {
            errors.rejectValue 'regionName', 'required',
                    ['regionName'] as Object[], null
        }
    }
}
