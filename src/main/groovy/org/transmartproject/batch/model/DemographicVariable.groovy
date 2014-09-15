package org.transmartproject.batch.model

import java.util.regex.Pattern

/**
 *
 */
enum DemographicVariable {

    AGE (
            column: 'age_in_years_num',
            defaultValue: 0,
            matchers: [
                    'AGE',
                    '.*\\(AGE\\)',
                    'AGE \\(YEAR\\)',
            ]
    ),

    GENDER (
            column: 'sex_cd',
            defaultValue: 'Unknown',
            matchers: [
                    'GENDER',
                    'SEX',
                    '.*\\(SEX\\)',
            ]
    ),

    RACE (
            column: 'race_cd',
            defaultValue: null,
            matchers: [
                    'RACE',
                    '.*\\(RACE\\)',
            ]
    );

    String column
    Object defaultValue
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