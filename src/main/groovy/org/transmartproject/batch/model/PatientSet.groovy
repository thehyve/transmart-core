package org.transmartproject.batch.model

import groovy.transform.ToString

/**
 *
 */
class PatientSet {

    Map<String,Patient> patientMap = [:]

    Patient getPatient(String id) {

        synchronized (patientMap) {
            Patient result = patientMap.get(id)
            if (!result) {
                result = new Patient(id: id)
                patientMap.put(id, result)
            }
            return result
        }
    }

}

@ToString(includes=['id','code'])
class Patient {
    String id
    Long code
    boolean persisted

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