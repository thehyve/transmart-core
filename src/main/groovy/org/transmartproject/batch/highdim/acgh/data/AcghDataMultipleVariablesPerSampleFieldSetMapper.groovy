package org.transmartproject.batch.highdim.acgh.data

import org.springframework.stereotype.Component
import org.transmartproject.batch.batchartifacts.AbstractMultipleVariablesPerSampleFieldSetMapper

/**
 * Implementation of {@link AbstractMultipleVariablesPerSampleFieldSetMapper} for {@link AcghDataValue}
 */
@Component
class AcghDataMultipleVariablesPerSampleFieldSetMapper
        extends AbstractMultipleVariablesPerSampleFieldSetMapper {
    @Override
    Object newInstance(String annotation, String sampleCode) {
        new AcghDataValue(regionName: annotation, sampleCode: sampleCode)
    }

    final Map<String, Closure> fieldSetters =
            [
                    flag       : { AcghDataValue instance, String value ->
                        instance.flag = (value.trim() ?: null) as Integer
                    },
                    chip       : { AcghDataValue instance, String value ->
                        instance.chip = (value.trim() ?: null) as Double
                    },
                    segmented  : { AcghDataValue instance, String value ->
                        instance.segmented = (value.trim() ?: null) as Double
                    },
                    probhomloss: { AcghDataValue instance, String value ->
                        instance.probHomLoss = (value.trim() ?: null) as Double
                    },
                    probloss   : { AcghDataValue instance, String value ->
                        instance.probLoss = (value.trim() ?: null) as Double
                    },
                    probnorm   : { AcghDataValue instance, String value ->
                        instance.probNorm = (value.trim() ?: null) as Double
                    },
                    probgain   : { AcghDataValue instance, String value ->
                        instance.probGain = (value.trim() ?: null) as Double
                    },
                    probamp    : { AcghDataValue instance, String value ->
                        instance.probAmp = (value.trim() ?: null) as Double
                    },
            ]

}
