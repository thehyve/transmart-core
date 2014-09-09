package org.transmartproject.batch.model

import com.google.common.base.Function
import groovy.transform.ToString
import org.transmartproject.batch.support.LineListener
import org.transmartproject.batch.support.MappingHelper

@ToString
class Variable implements Serializable {

    static final String SUBJ_ID = 'SUBJ_ID'
    static final String SITE_ID = 'SITE_ID'
    static final String VISIT_NAME = 'VISIT_NAME'

    static final List<String> RESERVED = [ SUBJ_ID, SITE_ID, VISIT_NAME ]

    String filename

    String categoryCode

    Integer columnNumber

    String dataLabel

    //the columns have fixed position, but not fixed names
    //most of the files have headers [filename, category_cd, col_nbr, data_label]
    //but some files dont, so we use position (not names) to identify columns
    private static fields = ['filename','categoryCode','columnNumber','dataLabel']

    static List<Variable> parse(InputStream input, LineListener listener) {
        MappingHelper.parseObjects(input, Variable, fields, listener)
    }

    static Variable forLine(String line) {
        MappingHelper.parseObject(line, Variable, fields)
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
