package org.transmartproject.batch.highdim.datastd

import org.transmartproject.batch.batchartifacts.AbstractMultipleVariablesPerSampleFieldSetMapper

/**
 * Implementation of {@link AbstractMultipleVariablesPerSampleFieldSetMapper} for {@link TripleStandardDataValue}
 */
class StandardMultipleVariablesPerSampleFieldSetMapper
        extends AbstractMultipleVariablesPerSampleFieldSetMapper<TripleStandardDataValue> {
    @Override
    TripleStandardDataValue newInstance(String annotation, String sampleCode) {
        new TripleStandardDataValue(annotation: annotation, sampleCode: sampleCode)
    }

    final Map<String, Closure> fieldSetters =
            [
                    val   : { TripleStandardDataValue instance, String value ->
                        instance.value = numberFormat.parse(value).doubleValue()
                    },
                    zscore: { TripleStandardDataValue instance, String value ->
                        instance.zscore = numberFormat.parse(value).doubleValue()
                    },
            ]
}
