package org.transmartproject.batch.highdim.rnaseq.data

import org.springframework.stereotype.Component
import org.transmartproject.batch.batchartifacts.AbstractMultipleVariablesPerSampleFieldSetMapper

/**
 * Implementation of {@link AbstractMultipleVariablesPerSampleFieldSetMapper} for {@link RnaSeqDataValue}
 */
@Component
class RnaSeqDataMultipleVariablesPerSampleFieldSetMapper
        extends AbstractMultipleVariablesPerSampleFieldSetMapper {
    @Override
    Object newInstance(String annotation, String sampleCode) {
        new RnaSeqDataValue(annotation: annotation, sampleCode: sampleCode)
    }

    final Map<String, Closure> fieldSetters =
            [
                    readcount          : { RnaSeqDataValue instance, String value ->
                        instance.readCount = (value.trim() ?: null) as Double
                    },
                    normalizedreadcount: { RnaSeqDataValue instance, String value ->
                        instance.value = (value.trim() ?: null) as Double
                    },
            ]
}
