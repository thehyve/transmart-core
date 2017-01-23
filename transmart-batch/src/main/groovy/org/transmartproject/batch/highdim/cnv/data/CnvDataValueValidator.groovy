package org.transmartproject.batch.highdim.cnv.data

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator

/**
 * Validates {@link CnvDataValue} objects.
 */
@Component
@Slf4j
class CnvDataValueValidator implements Validator {

    public static final double ERROR = 0.01d
    public static final FLAGS_RANGE = -2..2

    @Override
    boolean supports(Class<?> clazz) {
        clazz == CnvDataValue
    }

    @Override
    void validate(Object target, Errors errors) {
        CnvDataValue item = target

        if (!item.sampleCode?.trim()) {
            errors.rejectValue 'sampleCode', 'required',
                    ['sample'] as Object[], null
        }
        if (!item.regionName?.trim()) {
            errors.rejectValue 'regionName', 'required',
                    ['regionName'] as Object[], null
        }

        Map probabilities = [
                probHomLoss: item.probHomLoss,
                probLoss   : item.probLoss,
                probNorm   : item.probNorm,
                probGain   : item.probGain,
                probAmp    : item.probAmp]

        boolean probabilitiesSpecified = probabilities.values().any { it != null }
        if (probabilitiesSpecified) {
            Map incorrectProbabilities = probabilities.findAll {
                it.value != null && (it.value < 0 || it.value > 1)
            }
            incorrectProbabilities.each { String fieldName, Double fieldValue ->
                errors.rejectValue fieldName, 'notAllowedValue',
                        [fieldName, fieldValue, '0..1'] as Object[], null
            }

            boolean sumIsOne = Math.abs(1 - probabilities.values().sum { it ?: 0 }) < ERROR
            if (!sumIsOne) {
                errors.reject 'sumOfProbabilitiesIsNotOne'
            }
        }

        if (item.flag == null) {
            errors.rejectValue 'flag', 'required',
                    ['flag'] as Object[], null
        } else if (!(item.flag in FLAGS_RANGE)) {
            errors.rejectValue 'flag', 'notAllowedValue',
                    ['flag', item.flag, FLAGS_RANGE] as Object[], null
        }
    }
}
