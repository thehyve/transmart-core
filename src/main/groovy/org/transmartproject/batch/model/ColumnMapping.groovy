package org.transmartproject.batch.model

import groovy.transform.ToString


@ToString
class ColumnMapping {

    String filename
    String category
    Integer column
    String dataLabel
    String dataLabelSource
    String vocabularyCode

    private static fields = ['filename','category','column','dataLabel','dataLabelSource','vocabularyCode']

    static List<ColumnMapping> parse(InputStream input) {
        MappingHelper.parseObjects(input, ColumnMapping.class, fields)
    }

    static ColumnMapping forLine(String line) {
        MappingHelper.parseObject(line, ColumnMapping.class, fields)
    }

}
