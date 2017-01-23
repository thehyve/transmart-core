package org.transmartproject.batch.support

import org.springframework.batch.item.file.LineCallbackHandler

/**
 * Similar to {@link LineCallbackHandler}, but receives the line already
 * tokenized by tabs and returns the column names that the tokenizer should
 * then expect.
 */
interface TokenizerColumnsReplacingHeaderHandler {

    List<String> handleLine(List<String> tokenizedHeader)
}
