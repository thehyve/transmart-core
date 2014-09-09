package org.transmartproject.batch.model

import com.google.common.base.Function
import org.transmartproject.batch.support.LineListener
import org.transmartproject.batch.support.MappingHelper

/**
 *
 */
class WordMapping implements Serializable {
    String filename
    int column
    String originalValue
    String newValue

    private static fields = ['filename','column','originalValue','newValue']

    static List<WordMapping> parse(InputStream input, LineListener listener) {
        MappingHelper.parseObjects(input, WordMapping.class, fields, listener)
    }

    static WordMapping forLine(String line) {
        MappingHelper.parseObject(line, WordMapping.class, fields)
    }

}
