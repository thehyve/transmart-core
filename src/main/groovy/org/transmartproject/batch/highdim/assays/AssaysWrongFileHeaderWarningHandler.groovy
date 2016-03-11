package org.transmartproject.batch.highdim.assays

import groovy.util.logging.Slf4j
import org.springframework.batch.item.file.LineCallbackHandler
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.batch.item.file.transform.LineTokenizer
import org.transmartproject.batch.support.StringUtils

/**
 * Validates the headers in the subject sample mapping file and warns if columns 5-7 do not correspond to the new names.
 */
@Slf4j
class AssaysWrongFileHeaderWarningHandler
        implements LineCallbackHandler {

    private static final COLUMNS_TO_CHECK = [
            [zeroBasedNumber: 5, name: 'sample_type'],
            [zeroBasedNumber: 6, name: 'tissue_type'],
            [zeroBasedNumber: 7, name: 'time_point'],
    ]

    private final LineTokenizer lineTokenizer

    AssaysWrongFileHeaderWarningHandler(LineTokenizer lineTokenizer) {
        this.lineTokenizer = lineTokenizer
    }

    @Override
    void handleLine(String line) {
        FieldSet fieldSet = lineTokenizer.tokenize(line)

        COLUMNS_TO_CHECK.each { columnRenameFact ->
            String columnName = fieldSet.readString(columnRenameFact.zeroBasedNumber)
            if (!StringUtils.lookAlike(columnName, columnRenameFact.name)) {
                log.warn("The ${columnRenameFact.zeroBasedNumber + 1}th column is expected to have name" +
                        " ${columnRenameFact.name}, but the name in your file  is ${columnName}." +
                        " Please make sure the columns correspond.")
            }
        }
    }
}
