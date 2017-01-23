package org.transmartproject.batch.highdim.cnv.data

import com.google.common.collect.ImmutableSet
import org.transmartproject.batch.startup.ExternalJobParametersInternalInterface
import org.transmartproject.batch.startup.ExternalJobParametersModule
import org.transmartproject.batch.startup.InvalidParametersFileException

/**
 * Defines the ACGH parameters.
 */
class CnvDataParametersModule implements ExternalJobParametersModule {

    public final static String PROB_IS_NOT_1 = 'PROB_IS_NOT_1'
    public final static Set ALLOWED_PROB_IS_NOT_1_VALUES = ['WARN', 'ERROR'] as Set

    Set<String> supportedParameters = ImmutableSet.of(
            PROB_IS_NOT_1,
    )

    void validate(ExternalJobParametersInternalInterface ejp)
            throws InvalidParametersFileException {
        if (!ALLOWED_PROB_IS_NOT_1_VALUES.contains(ejp[PROB_IS_NOT_1])) {
            throw new InvalidParametersFileException(
                    "${PROB_IS_NOT_1} is ${ejp[PROB_IS_NOT_1]}." +
                            "It's expected to be one of the values: ${ALLOWED_PROB_IS_NOT_1_VALUES.join(', ')}")
        }
    }

    void munge(ExternalJobParametersInternalInterface ejp) throws InvalidParametersFileException {
        if (ejp[PROB_IS_NOT_1] == null) {
            ejp[PROB_IS_NOT_1] = 'ERROR'
        }
    }
}
