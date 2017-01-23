package org.transmartproject.batch.highdim.proteomics.platform

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator

/**
 * Validates {@link org.transmartproject.batch.highdim.proteomics.platform.ProteomicsAnnotationRow} objects.
 */
@Component
@JobScope
class ProteomicsAnnotationRowValidator implements Validator {

    @Override
    boolean supports(Class<?> clazz) {
        clazz == ProteomicsAnnotationRow
    }

    @Override
    @SuppressWarnings('ReturnNullFromCatchBlock')
    void validate(Object target, Errors errors) {
        assert target instanceof ProteomicsAnnotationRow

        if (!target.probesetId) {
            errors.rejectValue 'probesetId', 'required',
                    ['probesetId'] as Object[], null
        }

        if (!target.uniprotId) {
            errors.rejectValue 'uniprotId', 'required',
                    ['uniprotId'] as Object[], null
        }
    }
}
