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

    public static final double ERROR = 0.01d
    public static final Map FLAG_TO_PROBABILITY_FIELD_MAP = [
            (-2): 'probHomLoss',
            (-1): 'probLoss',
            (0) : 'probNorm',
            (1) : 'probGain',
            (2) : 'probAmp']
    public static final Map PROBABILITY_FIELD_TO_FLAG_MAP = FLAG_TO_PROBABILITY_FIELD_MAP
            .collectEntries { [(it.value): it.key] }

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

        Map probabilities = [
                probHomLoss: item.probHomLoss,
                probLoss   : item.probLoss,
                probNorm   : item.probNorm,
                probGain   : item.probGain,
                probAmp    : item.probAmp]
        boolean probabilitiesSpecified = probabilities.values().any { it != null }

        boolean probabilitiesAreValid = true
        if (probabilitiesSpecified) {
            Map incorrectProbabilities = probabilities.findAll {
                it.value != null && (it.value < 0 || it.value > 1)
            }
            incorrectProbabilities.each { String fieldName, Double fieldValue ->
                errors.rejectValue fieldName, 'notAllowedValue',
                        [fieldName, fieldValue, '0..1'] as Object[], null
            }
            probabilitiesAreValid &= !incorrectProbabilities

            boolean sumIsOne = Math.abs(1 - probabilities.values().sum { it ?: 0 }) < ERROR
            if (!sumIsOne) {
                errors.reject 'sumOfProbabilitiesIsNotOne'
            }
            probabilitiesAreValid &= sumIsOne
        }

        if (item.flag == null) {
            errors.rejectValue 'flag', 'required',
                    ['flag'] as Object[], null
        } else if (!(item.flag in FLAG_TO_PROBABILITY_FIELD_MAP.keySet())) {
            errors.rejectValue 'flag', 'notAllowedValue',
                    ['flag', item.flag, FLAG_TO_PROBABILITY_FIELD_MAP.keySet()] as Object[], null
        } else if (probabilitiesSpecified && probabilitiesAreValid) {
            Double maxProbability = probabilities.values().max()
            Set probFieldsFlagCandidates = probabilities.findAll { (maxProbability - it.value) < ERROR }.keySet()
            String flagSuggestedProbField = FLAG_TO_PROBABILITY_FIELD_MAP[item.flag]
            if (!probFieldsFlagCandidates.contains(flagSuggestedProbField)) {
                Set expectedFlags = probFieldsFlagCandidates
                        .collect { PROBABILITY_FIELD_TO_FLAG_MAP[it] }
                errors.rejectValue 'flag', 'expectedConstant',
                        [expectedFlags, item.flag] as Object[], null
            }
        }
    }
}
