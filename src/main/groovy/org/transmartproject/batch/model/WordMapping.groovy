package org.transmartproject.batch.model

import com.google.common.base.Function
import org.transmartproject.batch.support.LineListener
import org.transmartproject.batch.support.MappingHelper

/**
 * Represent a word replacement entry, as defined in a word mapping file.
 */
class WordMapping implements Serializable {
    String filename
    int column
    String originalValue
    String newValue

    private static fields = ['filename','column','originalValue','newValue']

    static List<WordMapping> parse(InputStream input, LineListener listener) {
        MappingHelper.parseObjects(input, LINE_MAPPER, listener)
    }

    static Function<String,WordMapping> LINE_MAPPER = new Function<String, WordMapping>() {
        @Override
        WordMapping apply(String input) {
            WordMapping result = MappingHelper.parseObject(input, WordMapping.class, fields)
            result.column-- //index is now 0 based
            result
        }
    }

}
