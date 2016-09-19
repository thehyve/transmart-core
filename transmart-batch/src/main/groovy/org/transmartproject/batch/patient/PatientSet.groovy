package org.transmartproject.batch.patient

import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Sequences
import org.transmartproject.batch.db.SequenceReserver

/**
 * Structure to hold all the patients found in database and data files, related to a study.
 * Creates patients on the fly.
 */
@Component
@JobScope
@Slf4j
class PatientSet {

    @Autowired
    SequenceReserver sequenceReserver

    private final Map<String, Patient> patientMap = [:]

    Patient getAt(String id) {
        Patient result = patientMap.get(id)
        if (result) {
            result
        } else {
            result = new Patient(id: id)
            patientMap[id] = result
        }
    }

    void leftShift(Patient patient) {
        assert patient.id != null

        patientMap[patient.id] = patient
    }

    Collection<Patient> getNewPatients() {
        patientMap.values().findAll { it.isNew() }
    }

    Collection<Patient> getAllPatients() {
        patientMap.values()
    }

    void reserveIdsFor(Patient patient) {
        if (!patient.isNew()) {
            log.warn("Could not rewrite id for patient that already has one (${patient.code}).")
            return
        }
        patient.code = sequenceReserver.getNext(Sequences.PATIENT)
    }

    void reserveIdsFor(Set<Patient> patients) {
        patients.each { reserveIdsFor it }
    }
}
