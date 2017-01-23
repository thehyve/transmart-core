package org.transmartproject.batch.highdim.cnv.data

import org.springframework.stereotype.Component
import org.transmartproject.batch.batchartifacts.AbstractMultipleVariablesPerSampleFieldSetMapper

/**
 * Implementation of {@link AbstractMultipleVariablesPerSampleFieldSetMapper} for {@link CnvDataValue}
 */
@Component
class CnvDataMultipleVariablesPerSampleFieldSetMapper
        extends AbstractMultipleVariablesPerSampleFieldSetMapper {
    @Override
    Object newInstance(String annotation, String sampleCode) {
        new CnvDataValue(regionName: annotation, sampleCode: sampleCode)
    }

    final Map<String, Closure> fieldSetters =
            [
                    flag       : { CnvDataValue instance, String value ->
                        instance.flag = (value.trim() ?: null) as Integer
                    },
                    chip       : { CnvDataValue instance, String value ->
                        instance.chip = (value.trim() ?: null) as Double
                    },
                    segmented  : { CnvDataValue instance, String value ->
                        instance.segmented = (value.trim() ?: null) as Double
                    },
                    probhomloss: { CnvDataValue instance, String value ->
                        instance.probHomLoss = (value.trim() ?: null) as Double
                    },
                    probloss   : { CnvDataValue instance, String value ->
                        instance.probLoss = (value.trim() ?: null) as Double
                    },
                    probnorm   : { CnvDataValue instance, String value ->
                        instance.probNorm = (value.trim() ?: null) as Double
                    },
                    probgain   : { CnvDataValue instance, String value ->
                        instance.probGain = (value.trim() ?: null) as Double
                    },
                    probamp    : { CnvDataValue instance, String value ->
                        instance.probAmp = (value.trim() ?: null) as Double
                    },
            ]

}
