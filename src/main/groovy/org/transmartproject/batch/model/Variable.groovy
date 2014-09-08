package org.transmartproject.batch.model

import com.google.common.base.Function
import groovy.transform.ToString
import org.transmartproject.batch.support.MappingHelper

@ToString
class Variable implements Serializable {

    String filename

    String categoryCode

    Integer columnNumber

    String dataLabel

    //the columns have fixed position, but not fixed names
    //most of the files have headers [filename, category_cd, col_nbr, data_label]
    //but some files dont, so we use position (not names) to identify columns
    private static fields = ['filename','categoryCode','columnNumber','dataLabel']

    static List<Variable> parse(InputStream input) {
        MappingHelper.parseObjects(input, Variable, fields)
    }

    static Variable forLine(String line) {
        MappingHelper.parseObject(line, Variable, fields)
    }

    static Function<File, List<Variable>> READER = new Function<File, List<Variable>>() {
        @Override
        List<Variable> apply(File input) {
            parse(input.newInputStream())
        }
    }

    static void validateDataFiles(Set<File> list) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException('No data files defined')
        }
        list.each {
            if (!it.exists()) {
                throw new IllegalArgumentException("Data file $it.absolutePath not found")
            }
        }
    }

    static Set<File> getDataFiles(File folder, List<Variable> list) {
        list.collect { it.filename }.toSet().collect { new File(folder, it) }
    }

}
