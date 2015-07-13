package org.transmartproject.batch.patient

import org.transmartproject.batch.concept.ConceptType

import java.util.regex.Pattern

/**
 * Represent a demographic variable, that matches a column in table patient_dimension
 */
enum DemographicVariable {

    AGE(
            column: 'age_in_years_num',
            defaultValue: 0,
            type: ConceptType.NUMERICAL,
            matchers: [
                    'AGE',
                    '.*\\(AGE\\)',
                    'AGE \\(YEAR\\)',
            ]
    ),

    GENDER(
            column: 'sex_cd',
            defaultValue: 'Unknown',
            type: ConceptType.CATEGORICAL,
            matchers: [
                    'GENDER',
                    'SEX',
                    '.*\\(SEX\\)',
            ]
    ),

    RACE(
            column: 'race_cd',
            defaultValue: null,
            type: ConceptType.CATEGORICAL,
            matchers: [
                    'RACE',
                    '.*\\(RACE\\)',
            ]
    )

    String column
    Object defaultValue
    ConceptType type
    List<String> matchers

    boolean matches(String name) {
        matchers.find {
            Pattern.matches(it, name)
        }
    }

    static DemographicVariable getMatching(String variableName) {
        String name = variableName.toUpperCase()
        DemographicVariable.values().find { it.matches(name) }
    }

}
