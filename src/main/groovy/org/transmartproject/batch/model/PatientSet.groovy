package org.transmartproject.batch.model

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

class Patient {
    String id
    Long code
}