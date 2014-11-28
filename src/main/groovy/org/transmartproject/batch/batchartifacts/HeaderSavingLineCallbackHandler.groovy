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
        def previousValue = stepExecution.executionContext.get(KEY)
        if (stepExecution.executionContext.get(KEY) != null) {
            throw new IllegalStateException("Told to read skipped line with " +
                    "content '$line', but I remember having gotten one " +
                    "before, which I tokenized as: $previousValue")
        }

        List<String> headerNames =
                new DelimitedLineTokenizer(
                        delimiter: DelimitedLineTokenizer.DELIMITER_TAB,
                ).tokenize(line).values

        stepExecution.executionContext.put(KEY, headerNames as List)
    }
}
