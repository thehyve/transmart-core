package org.transmartproject.batch.clinical

import groovy.transform.ToString
import org.transmartproject.batch.model.ConceptNode
import org.transmartproject.batch.model.Patient
import org.transmartproject.batch.model.Variable

/**
 *
 */
class FactRowSet {
    String studyId
    Patient patient
    String siteId
    String visitName
    //String controlledVocabularyCode //NOT USED

    private Map<Variable,Entry> variableValueMap = [:]

    List<FactRow> getFactRows() {
        variableValueMap.collect {
            new FactRow(
                studyId: studyId,
                subjectId: patient.id,
                siteId: siteId,
                visitName: visitName,
                dataLabel: it.key.dataLabel,
                categoryCode: it.key.categoryCode,
                value: it.value.value,
            )
        }
    }

    void addValue(Variable variable, String value) {
        ConceptNode concept = variable.getValueConcept(value)
        concept.addSubject(patient.id)
        variableValueMap.put(variable, new Entry(variable: variable, value:value, concept: concept))
    }

    List<ObservationFact> getObservationFacts() {
        variableValueMap.values().collect {

            new ObservationFact(
                    sourcesystem_cd: studyId,
                    //encounter_num: patient.code,
                    patient_num: patient.code,
                    concept_cd: it.concept.code,
                    //start_date:
                    valtype_cd: it.getValueTypeCode(),
                    tval_char: it.getStringValue(),
                    nval_num: it.getNumericValue(),
            )
        }
    }

    private class Entry {
        Variable variable
        String value
        ConceptNode concept

        String getValueTypeCode() {
            switch (variable.type) {
                case Variable.VariableType.NUMERICAL:
                    return 'N'
                case Variable.VariableType.CATEGORICAL:
                    return 'T'
            }
        }

        String getStringValue() {
            switch (variable.type) {
                case Variable.VariableType.NUMERICAL:
                    return 'E' //@todo verify logic
                case Variable.VariableType.CATEGORICAL:
                    return value
            }
        }

        Double getNumericValue() {
            switch (variable.type) {
                case Variable.VariableType.NUMERICAL:
                    return Double.valueOf(value)
                case Variable.VariableType.CATEGORICAL:
                    return null
            }
        }
    }
}
//to insert into observation_fact (or any temporary table for duplicate check)
@ToString(includes=['patient_num','concept_cd','tval_char','nval_num'])
class ObservationFact {
    Long encounter_num
    Long patient_num
    String concept_cd
    final String provider_id = '@'
    Date start_date
    final String modifier_cd = '@'
    final Integer instance_num = 1
    String valtype_cd
    String tval_char
    Double nval_num
    String valueflag_cd
    Double quantity_num
    String units_cd
    Date end_date
    final String location_cd = '@'
    String observation_blob
    Double confidence_num
    Date update_date
    Date download_date
    Date import_date
    String sourcesystem_cd
    Long upload_id
    String sample_cd
}

//to insert into lt_src_clinical_data (to use with the stored procedure)
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
