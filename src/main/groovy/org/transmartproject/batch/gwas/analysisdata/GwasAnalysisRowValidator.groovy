package org.transmartproject.batch.gwas.analysisdata

import groovy.transform.CompileStatic
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.transmartproject.batch.support.ConfigurableLengthSemanticsTrait

/**
 * Validates {@link GwasAnalysisRowValidator}.
 * Using JSR 303 validators is unfortunately too slow.
 */
@Component
@CompileStatic
class GwasAnalysisRowValidator implements Validator, ConfigurableLengthSemanticsTrait {

    private static final int MAX_LENGTH_OF_RS_ID = 50

    @Override
    boolean supports(Class<?> clazz) {
        clazz == GwasAnalysisRow
    }

    @Override
    void validate(Object target, Errors errors) {
        GwasAnalysisRow row = (GwasAnalysisRow) target

        if (!row.rsId) {
            errors.rejectValue(
                    'rsId', 'required', ['filename'] as Object[], null)
        } else if (lengthOf(row.rsId) > MAX_LENGTH_OF_RS_ID) {
            errors.rejectValue 'rsId', 'maxSizeExceeded',
                    ['rsId', lengthOf(row.rsId), MAX_LENGTH_OF_RS_ID] as Object[],
                    null
        }

        if (row.allele1 != null && lengthOf(row.allele1) != 1) {
            errors.rejectValue 'allele1', 'maxSizeExceeded',
                    ['allele1', lengthOf(row.allele1), 1] as Object[],
                    null
        }
        if (row.allele2 != null && lengthOf(row.allele2) != 1) {
            errors.rejectValue 'allele2', 'maxSizeExceeded',
                    ['allele2', lengthOf(row.allele2), 1] as Object[],
                    null
        }

        if (row.pValue != null && row.pValue <= 0) {
            errors.rejectValue(
                    'pValue', 'mustBePositive', [] as Object[], null)
        }

        if (row.standardError != null && row.pValue <= 0) {
            errors.rejectValue(
                    'standardError', 'mustBePositive', [] as Object[], null)
        }
    }
}
