package org.transmartproject.batch.clinical

import org.transmartproject.batch.model.Variable

/**
 *
 */
class FactRowSet {
    String studyId
    String subjectId
    String siteId
    String visitName
    //String controlledVocabularyCode //NOT USED

    Map<Variable,String> variableValueMap

    List<FactRow> getFactRows() {
        variableValueMap.collect {
            new FactRow(
                studyId: studyId,
                subjectId: subjectId,
                siteId: siteId,
                visitName: visitName,
                dataLabel: it.key.dataLabel,
                categoryCode: it.key.categoryCode,
                value: it.value,
            )
        }
    }

}

class FactRow {
    String studyId
    String subjectId
    String siteId
    String visitName
    //String controlledVocabularyCode

    String dataLabel
    String categoryCode
    String value
}
