package org.transmartproject.batch.batchartifacts

import org.springframework.batch.core.StepExecution
import org.springframework.batch.item.file.LineCallbackHandler
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer
import org.springframework.beans.factory.annotation.Value

/**
 * Is passed the skipped TSV file header and saves it in the step execution
 * context.
 */
class HeaderSavingLineCallbackHandler implements LineCallbackHandler {

    public static final String KEY = 'tsv.header'

    @Value('#{stepExecution}')
    StepExecution stepExecution

    DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(
            delimiter: DelimitedLineTokenizer.DELIMITER_TAB,
    )

    @Override
    void handleLine(String line) {
        List<String> previousHeaders = stepExecution.executionContext.get(KEY) as List
        List<String> newHeaders = tokenizer.tokenize(line).values as List

        if (previousHeaders && previousHeaders != newHeaders) {
            throw new IllegalStateException("Told to read skipped line with " +
                    "content '$line', but I remember having gotten a " +
                    "different one before, which I tokenized as: " +
                    "$previousHeaders")
        }
        Set<String> appearMoreThenOnceColumnNames = newHeaders
                .groupBy { it }
                .findAll { it.value.size() > 1 }
                .keySet()
        if (appearMoreThenOnceColumnNames) {
            throw new IllegalStateException('Following column names appear more then once: '
                    + appearMoreThenOnceColumnNames.join(', '))
        }

        stepExecution.executionContext.put(KEY, newHeaders)
    }
}
