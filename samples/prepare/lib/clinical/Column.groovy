package lib.clinical

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode(includes = ['categoryCode', 'dataLabel'])
@ToString(includes = ['categoryCode', 'dataLabel'])
class Column implements Comparable<Column> {
    public final static String SUBJECT_ID_LABEL = 'SUBJ_ID'
    public final static String AGE_LABEL = 'AGE'
    public final static String SEX_LABEL = 'SEX'
    public final static String RACE_LABEL = 'RACE'

    private final Map PRIORITY_LABEL_MAP = [
            1: SUBJECT_ID_LABEL,
            2: AGE_LABEL,
            3: SEX_LABEL,
            4: RACE_LABEL
    ]

    String categoryCode
    String dataLabel
    String dataLabelSource = ''
    String controlledVocabularyCode = ''

    @Override
    int compareTo(Column other) {
        other.priority <=> priority ?:
                other.categoryCode <=> categoryCode ?:
                other.dataLabel <=> dataLabel
    }

    private Integer getPriority() {
        PRIORITY_LABEL_MAP[dataLabel]
    }
}
