package org.transmartproject.batch.highdim.metabolomics.platform

import groovy.transform.CompileStatic
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.transmartproject.batch.support.ConfigurableLengthSemanticsTrait

/**
 * Validates a {@link MetabolomicsAnnotationRow}.
 */
@Component
@CompileStatic
class MetabolomicsAnnotationRowValidator implements
        Validator, ConfigurableLengthSemanticsTrait {

    public static final int MAX_LENGTH_OF_BIOCHEMICAL = 200

    public static final int MAX_LENGTH_OF_SUB_PATHWAY = 200

    public static final int MAX_LENGTH_OF_SUPER_PATHWAY = 200
    public static final int MAX_LENGTH_OF_HMDB = 50

    @Override
    boolean supports(Class<?> clazz) {
        clazz == MetabolomicsAnnotationRow
    }

    @Override
    void validate(Object target, Errors errors) {
        MetabolomicsAnnotationRow row = (MetabolomicsAnnotationRow) target

        if (!row.biochemical) {
            errors.rejectValue 'biochemical', 'required',
                    ['biochemical'] as Object[], null
        }
        int biochemicalLen = lengthOf(row.biochemical)
        if (biochemicalLen > MAX_LENGTH_OF_BIOCHEMICAL) {
            errors.rejectValue 'biochemical', 'maxSizeExceeded',
                    ['biochemical', biochemicalLen,
                     MAX_LENGTH_OF_BIOCHEMICAL] as Object[], null
        }

        if (row.subPathway) {
            int subPathwayLen = lengthOf(row.subPathway)
            if (subPathwayLen > MAX_LENGTH_OF_SUB_PATHWAY) {
                errors.rejectValue 'subPathway', 'maxSizeExceeded',
                        ['subPathway', subPathwayLen,
                         MAX_LENGTH_OF_SUB_PATHWAY] as Object[], null
            }
        }

        if (row.superPathway) {
            if (!row.subPathway) {
                errors.rejectValue 'superPathway', 'superPathwayWithoutSubPath',
                        [] as Object[], null
            }
            int superPathwayLen = lengthOf(row.superPathway)
            if (superPathwayLen > MAX_LENGTH_OF_SUPER_PATHWAY) {
                errors.rejectValue 'superPathway', 'maxSizeExceeded',
                        ['superPathway', superPathwayLen,
                         MAX_LENGTH_OF_SUPER_PATHWAY] as Object[], null
            }
        }

        def hmdbIdLen = row.hmdbId ? lengthOf(row.hmdbId) : 0
        if (hmdbIdLen > MAX_LENGTH_OF_HMDB) {
            errors.rejectValue 'superPathway', 'maxSizeExceeded',
                    ['superPathway', hmdbIdLen, MAX_LENGTH_OF_HMDB] as Object[], null
        }
    }
}
