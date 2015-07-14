package org.transmartproject.batch.clinical.facts

import com.google.common.base.Function
import org.transmartproject.batch.support.LineListener
import org.transmartproject.batch.support.MappingHelper

/**
 * Represent a word replacement entry, as defined in a word mapping file.
 */
class WordMapping implements Serializable {

    private static final long serialVersionUID = 1L

    String filename
    int column /* 0-based, unlike in the source file */
    String originalValue
    String newValue

    private final static FIELDS = ['filename', 'column', 'originalValue', 'newValue']

    static List<WordMapping> parse(InputStream input, LineListener listener) {
        MappingHelper.parseObjects(input, LINE_MAPPER, listener)
    }

    @SuppressWarnings('FieldName')
    final static Function<String, WordMapping> LINE_MAPPER = new Function<String, WordMapping>() {
        @Override
        WordMapping apply(String input) {
            WordMapping result = MappingHelper.parseObject(input, WordMapping, FIELDS)
            result.column-- //index is now 0 based
            result
        }
    }

}
