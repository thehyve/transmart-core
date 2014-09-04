package org.transmartproject.batch.model

import com.google.common.base.Function
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

    static Function<File, List<ColumnMapping>> READER = new Function<File, List<ColumnMapping>>() {
        @Override
        List<ColumnMapping> apply(File input) {
            parse(input.newInputStream())
        }
    }

}
