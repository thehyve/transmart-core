package org.transmartproject.db.i2b2data

import static org.transmartproject.db.TestDataHelper.save

class I2b2Data {

    String trialName
    List<PatientDimension> patients
    List<PatientTrialCoreDb> patientTrials

    static I2b2Data createDefault() {
        String trialName = 'STUDY1'
        List<PatientDimension> patients = createTestPatients(3, -100, trialName)
        List<PatientTrialCoreDb> patientTrials = createPatientTrialLinks(patients, trialName)
        new I2b2Data(trialName: trialName, patients: patients, patientTrials: patientTrials)
    }

    static List<PatientDimension> createTestPatients(int n, long baseId, String trialName = 'SAMP_TRIAL') {
        (1..n).collect { int i ->
            def p = new PatientDimension(sourcesystemCd: "$trialName:SUBJ_ID_$i")
            p.id = baseId - i
            p
        }
    }

    static List<PatientTrialCoreDb> createPatientTrialLinks(Collection<PatientDimension> patients, String trialName) {
        patients.collect {
            new PatientTrialCoreDb(patient: it, study: trialName)
        }
    }

    void saveAll() {
        save patients
        save patientTrials
    }

}
