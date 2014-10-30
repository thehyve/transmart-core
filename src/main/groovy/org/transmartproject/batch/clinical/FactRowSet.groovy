package org.transmartproject.batch.clinical

import org.transmartproject.batch.model.ConceptNode
import org.transmartproject.batch.model.Patient
import org.transmartproject.batch.model.Variable
import org.transmartproject.batch.model.VariableType

/**
 * Contains the transformed meaningful information from one data Row</br>
 * This includes the Patient and values for all Variables in a row of a file
 */
class FactRowSet {
    String studyId
    Patient patient
    String siteId
    String visitName

    private final Map<Variable,Entry> variableValueMap = [:]

    /**
     * Sets the value for a given variable, returning the value concept
     * @param variable
     * @param value
     * @return value concept
     */
    ConceptNode addValue(Variable variable, String value) {
        ConceptNode concept = variable.getValueConcept(value)
        variableValueMap.put(variable, new Entry(variable: variable, value:value, concept: concept))
        concept
    }

    List<Map<String,Object>> getObservationFactRows() {

        Date dt = new Date()
        variableValueMap.values().collect {
            [
                    sourcesystem_cd: studyId,
                    encounter_num: patient.code,
                    patient_num: patient.code,
                    concept_cd: it.concept.code,
                    //start_date: new Date(Long.MAX_VALUE), //doesn't work
                    valtype_cd: it.valueTypeCode,
                    tval_char: it.stringValue,
                    nval_num: it.numericValue,
                    import_date: dt,

                    provider_id: '@',
                    location_cd: '@',
                    modifier_cd: '@',
                    valueflag_cd: '@',
                    instance_num: 1,
            ]
        }
    }

    private class Entry {
        Variable variable
        String value
        ConceptNode concept

        String getValueTypeCode() {
            switch (variable.type) {
                case VariableType.NUMERICAL:
                    return 'N'
                case VariableType.CATEGORICAL:
                    return 'T'
            }
        }

        String getStringValue() {
            switch (variable.type) {
                case VariableType.NUMERICAL:
                    return 'E' //@todo verify logic
                case VariableType.CATEGORICAL:
                    return value
            }
        }

        Double getNumericValue() {
            switch (variable.type) {
                case VariableType.NUMERICAL:
                    return Double.valueOf(value)
                case VariableType.CATEGORICAL:
                    return null
            }
        }
    }
}
