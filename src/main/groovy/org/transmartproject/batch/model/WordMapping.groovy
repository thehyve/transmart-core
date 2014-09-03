package org.transmartproject.batch.model

/**
 *
 */
class WordMapping {
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

}
