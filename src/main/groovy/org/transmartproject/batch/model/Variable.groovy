package org.transmartproject.batch.model

import com.google.common.base.Function
import groovy.transform.ToString
import org.transmartproject.batch.support.LineListener
import org.transmartproject.batch.support.MappingHelper

/**
 * Represents a Variable, as defined in column map file
 */
@ToString
class Variable implements Serializable {

    private static final long serialVersionUID = 1L

    static final String SUBJ_ID = 'SUBJ_ID'
    static final String STUDY_ID = 'STUDY_ID'
    static final String SITE_ID = 'SITE_ID'
    static final String VISIT_NAME = 'VISIT_NAME'
    static final String OMIT = 'OMIT'

    static final List<String> RESERVED = [ SUBJ_ID, STUDY_ID, SITE_ID, VISIT_NAME, OMIT ]

    //the columns have fixed position, but not fixed names
    //most of the files have headers [filename, category_cd, col_nbr, data_label]
    //but some files dont, so we use position (not names) to identify columns
    static final FIELDS = ['filename','categoryCode','columnNumber','dataLabel']

    String filename

    String categoryCode

    Integer columnNumber

    String dataLabel

    ConceptNode concept

    VariableType type

    DemographicVariable demographicVariable

    ConceptNode getValueConcept(String value) {

        if (!type) {
            //risky assumption: we use the fist value to decide the variable type
            try {
                Double.parseDouble(value)
                type = VariableType.NUMERICAL
            } catch (NumberFormatException ex) {
                type = VariableType.CATEGORICAL
            }
        }

        switch (type) {
            case VariableType.NUMERICAL:
                //numerical: just return the current concept
                return concept
            case VariableType.CATEGORICAL:
                //categorical: find/create a new concept
                return concept.find(value)
            default:
                throw new IllegalArgumentException('not supported')
        }
    }

    Object getValue(String source) {
        if (!source) {
            return null
        }

        switch (type) {
            case VariableType.NUMERICAL:
                return Double.valueOf(source)
            case VariableType.CATEGORICAL:
                return source
            default:
                throw new IllegalArgumentException('not supported')
        }
    }

    static List<Variable> parse(InputStream input, LineListener listener, ConceptTree conceptTree) {
        MappingHelper.parseObjects(input, new VariableLineMapper(tree: conceptTree), listener)
    }

}

enum VariableType {
    NUMERICAL,
    CATEGORICAL,
}

class VariableLineMapper implements Function<String,Variable> {

    ConceptTree tree

    @Override
    Variable apply(String input) {
        Variable result = MappingHelper.parseObject(input, Variable, Variable.FIELDS)
        result.columnNumber-- //index is now 0 based
        if (!Variable.RESERVED.contains(result.dataLabel)) {
            //resolve the concept
            ConceptNode concept = tree.study.find(result.categoryCode, result.dataLabel)
            result.concept = concept
            concept.variable = result
        }
        result
    }
}

