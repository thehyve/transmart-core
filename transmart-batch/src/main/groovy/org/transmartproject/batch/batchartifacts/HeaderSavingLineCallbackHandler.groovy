package org.transmartproject.batch.batchartifacts

import org.springframework.batch.core.StepExecution
import org.springframework.batch.item.file.LineCallbackHandler
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer
import org.springframework.beans.factory.annotation.Value
import org.transmartproject.batch.beans.StepScopeInterfaced

/**
 * Is passed the skipped TSV file header and saves it in the step execution
 * context.
 */
@StepScopeInterfaced
class HeaderSavingLineCallbackHandler implements LineCallbackHandler {

    public static final String KEY = 'tsv.header'

    @Value('#{stepExecution}')
    StepExecution stepExecution

    @Override
    void handleLine(String line) {
        List<String> previousHeaders = stepExecution.executionContext.get(KEY)
        List<String> newHeaders =
                new DelimitedLineTokenizer(
                        delimiter: DelimitedLineTokenizer.DELIMITER_TAB,
                ).tokenize(line).values as List

        if (previousHeaders && previousHeaders != newHeaders) {
            throw new IllegalStateException("Told to read skipped line with " +
                    "content '$line', but I remember having gotten a " +
                    "different one before, which I tokenized as: " +
                    "$previousHeaders")
        }

        stepExecution.executionContext.put(KEY, newHeaders)
    }
}
