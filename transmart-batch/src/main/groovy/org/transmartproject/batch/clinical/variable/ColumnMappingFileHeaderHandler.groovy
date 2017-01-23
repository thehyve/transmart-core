package org.transmartproject.batch.clinical.variable

import groovy.util.logging.Slf4j
import org.springframework.batch.item.ParseException
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.batch.support.TokenizerColumnsReplacingHeaderHandler

/**
 * Validates the headers in the column mapping file and sets the expectation
 * of the number of columns for the tokenizer.
 */
@Component
@Scope('singleton')
@Slf4j
class ColumnMappingFileHeaderHandler
        implements TokenizerColumnsReplacingHeaderHandler {

    @Override
    List<String> handleLine(List<String> values) {
        if (values.size() == 4) {
            log.debug("Column mapping file's header has 4 columns, OK. " +
                    "Expected these to be ${ClinicalVariable.FIELDS[0..3]}")
        } else if (values.size() == 6) {
            log.warn("Column mapping file's header has 6 columns. This is " +
                    "accepted, but the 5th and 6th columns will be ignored")
        } else if (values.size() == 7) {
            log.warn("Column mapping file's header has 7 columns. This is " +
                    "accepted, but the 5th and 6th columns will be ignored")
        } else {
            throw new ParseException("Expected the column mapping file " +
                    "header to have either 4, 6 or 7 columns. " +
                    "Got ${values} (size ${values.size()})")
        }

        ClinicalVariable.FIELDS[0..<values.size()]
    }
}
