package org.transmartproject.batch.highdim.acgh.data

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator

/**
 * Validates {@link org.transmartproject.batch.highdim.acgh.data.AcghDataValue} objects.
 */
@Component
@Slf4j
class AcghDataValueValidator implements Validator {

    public static final List ALLOWED_FLAG_VALUES = -2..2
    public static final double ERROR = 0.01d

    @Override
    boolean supports(Class<?> clazz) {
        clazz == AcghDataValue
    }

    @Override
    void validate(Object target, Errors errors) {
        AcghDataValue item = target

        if (!item.sampleCode?.trim()) {
            errors.rejectValue 'sampleCode', 'required',
                    ['sample'] as Object[], null
        }
        if (!item.regionName?.trim()) {
            errors.rejectValue 'regionName', 'required',
                    ['regionName'] as Object[], null
        }

        List probabilities = [item.probHomLoss, item.probLoss, item.probNorm, item.probGain, item.probAmp]
        boolean probabilitiesSpecified = probabilities.any { it != null }

        boolean one = true
        if (probabilitiesSpecified) {
            one = Math.abs(1 - probabilities.sum { it ?: 0 }) < ERROR
            if (!one) {
                errors.reject 'sumOfProbabilitiesIsNotOne'
            }
        }

        if (item.flag == null) {
            errors.rejectValue 'flag', 'required',
                    ['flag'] as Object[], null
        } else if (!(item.flag in ALLOWED_FLAG_VALUES)) {
            errors.rejectValue 'flag', 'notAllowedValue',
                    ['flag', item.flag, ALLOWED_FLAG_VALUES.toString()] as Object[], null
        } else if (probabilitiesSpecified && one) {
            int expectedFlag = probabilities.indexOf(probabilities.max()) - 2
            if (item.flag != expectedFlag) {
                errors.rejectValue 'flag', 'expectedConstant',
                        [expectedFlag, item.flag] as Object[], null
            }
        }
    }
}
