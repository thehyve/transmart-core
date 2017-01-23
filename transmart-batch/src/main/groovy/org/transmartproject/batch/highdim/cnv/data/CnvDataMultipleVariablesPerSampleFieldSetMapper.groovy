package org.transmartproject.batch.highdim.cnv.data

import org.transmartproject.batch.batchartifacts.AbstractMultipleVariablesPerSampleFieldSetMapper

/**
 * Implementation of {@link AbstractMultipleVariablesPerSampleFieldSetMapper} for {@link CnvDataValue}
 */
class CnvDataMultipleVariablesPerSampleFieldSetMapper
        extends AbstractMultipleVariablesPerSampleFieldSetMapper<CnvDataValue> {
    @Override
    CnvDataValue newInstance(String annotation, String sampleCode) {
        new CnvDataValue(regionName: annotation, sampleCode: sampleCode)
    }

    final Map<String, Closure> fieldSetters =
            [
                    flag       : { CnvDataValue instance, String value ->
                        instance.flag = numberFormat.parse(value).intValue()
                    },
                    chip       : { CnvDataValue instance, String value ->
                        instance.chip = numberFormat.parse(value).doubleValue()
                    },
                    segmented  : { CnvDataValue instance, String value ->
                        instance.segmented = numberFormat.parse(value).doubleValue()
                    },
                    probhomloss: { CnvDataValue instance, String value ->
                        instance.probHomLoss = numberFormat.parse(value).doubleValue()
                    },
                    probloss   : { CnvDataValue instance, String value ->
                        instance.probLoss = numberFormat.parse(value)?.doubleValue()
                    },
                    probnorm   : { CnvDataValue instance, String value ->
                        instance.probNorm = numberFormat.parse(value).doubleValue()
                    },
                    probgain   : { CnvDataValue instance, String value ->
                        instance.probGain = numberFormat.parse(value).doubleValue()
                    },
                    probamp    : { CnvDataValue instance, String value ->
                        instance.probAmp = numberFormat.parse(value).doubleValue()
                    },
            ]

}
