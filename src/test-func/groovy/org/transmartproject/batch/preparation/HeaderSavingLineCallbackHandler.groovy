package org.transmartproject.batch.preparation

import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.file.LineCallbackHandler
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Is passed the skipped TSV file header and saves it in the step execution
 * context.
 */
@StepScope
@Component
class HeaderSavingLineCallbackHandler implements LineCallbackHandler {

    public static final String KEY = 'tsv.header'

    @Value('#{stepExecution}')
    StepExecution stepExecution

    @Override
    void handleLine(String line) {
        List<String> headerNames =
                new DelimitedLineTokenizer(
                        delimiter: DelimitedLineTokenizer.DELIMITER_TAB,
                ).tokenize(line).values*.toLowerCase()

        stepExecution.executionContext.put(KEY, headerNames as List)
    }
}
