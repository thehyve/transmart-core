package org.transmartproject.batch.model

import groovy.transform.ToString

/**
 *
 */
class PatientSet {

    Map<String,Patient> patientMap = [:].asSynchronized()

    Patient getPatient(String id) {

        Patient result = patientMap.get(id)
        if (!result) {
            result = new Patient(id: id)
            patientMap.put(id, result)
        }
        return result
    }

}

@ToString(includes=['id','code'])
class Patient {
    String id
    Long code
    boolean isNew = true //new by default

    Map<Variable,String> demographicValues = [:]

    Object getDemographicValue(Variable var) {
        String str = demographicValues.get(var)
        if (str) {
            return var.getValue(str)
        } else {
            return var.demographicVariable.defaultValue
        }
    }

}