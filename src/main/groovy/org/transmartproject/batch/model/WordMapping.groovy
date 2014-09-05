package org.transmartproject.batch.model

import com.google.common.base.Function

/**
 *
 */
class WordMapping implements Serializable {
    String filename
    int column
    String originalValue
    String newValue

    private static fields = ['filename','column','originalValue','newValue']

    static List<WordMapping> parse(InputStream input) {
        MappingHelper.parseObjects(input, WordMapping.class, fields)
    }

    static WordMapping forLine(String line) {
        MappingHelper.parseObject(line, WordMapping.class, fields)
    }

    static Function<File, List<WordMapping>> READER = new Function<File, List<WordMapping>>() {
        @Override
        List<WordMapping> apply(File input) {
            if (input) {
                return parse(input.newInputStream())
            }
            null //no file, no results
        }
    }

}
