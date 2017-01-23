package org.transmartproject.batch.highdim.rnaseq.data

import org.transmartproject.batch.batchartifacts.AbstractMultipleVariablesPerSampleFieldSetMapper

/**
 * Implementation of {@link AbstractMultipleVariablesPerSampleFieldSetMapper} for {@link RnaSeqDataValue}
 */
class RnaSeqDataMultipleVariablesPerSampleFieldSetMapper
        extends AbstractMultipleVariablesPerSampleFieldSetMapper<RnaSeqDataValue> {
    @Override
    RnaSeqDataValue newInstance(String annotation, String sampleCode) {
        new RnaSeqDataValue(annotation: annotation, sampleCode: sampleCode)
    }

    final Map<String, Closure> fieldSetters =
            [
                    readcount          : { RnaSeqDataValue instance, String value ->
                        instance.readCount = numberFormat.parse(value).doubleValue()
                    },
                    normalizedreadcount: { RnaSeqDataValue instance, String value ->
                        instance.value = numberFormat.parse(value).doubleValue()
                    },
                    zscore: { RnaSeqDataValue instance, String value ->
                        instance.zscore = numberFormat.parse(value).doubleValue()
                    },
            ]
}
