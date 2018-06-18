package org.transmartproject.interceptors

import groovy.transform.CompileStatic

@CompileStatic
class PatientQueryAuditInterceptor extends AuditInterceptor {

    PatientQueryAuditInterceptor() {
        match(controller: ~/patientQuery/,
                action: ~/listPatients|findPatient|findPatientSet|findPatientSets|createPatientSet/)
    }

    boolean after() {
        if (actionName in ['listPatients', 'findPatient']) {
            return report("Patients data retrieval", "User (IP: ${IP}) made a patients data request.")
        } else if (actionName == 'createPatientSet') {
            return report("Patient set creation", "User (IP: ${IP}) created a new patient set.")
        } else {
            return report("Patient sets data retrieval",
                    "User (IP: ${IP}) made a patient sets data request.")
        }
    }

}
