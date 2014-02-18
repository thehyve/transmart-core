package org.transmartproject.db.i2b2data

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

class I2b2Data {

    String trialName

    I2b2Data(String trialName) {
        this.trialName = trialName
    }

    List<PatientDimension> patients = createTestPatients(3, -100, trialName)

    List<PatientTrialCoreDb> patientTrials = createPatientTrialLinks(patients, trialName)

    static List<PatientDimension> createTestPatients(int n, long baseId, String trialName) {
        (1..n).collect { int i ->
            def p = new PatientDimension(sourcesystemCd: "$trialName:SUBJ_ID_$i")
            p.id = baseId - i
            p
        }
    }

    static List<PatientTrialCoreDb> createPatientTrialLinks(Collection<PatientDimension> patients,
                                                            String trialName) {
        patients.collect {
            new PatientTrialCoreDb(patient: it, study: trialName)
        }
    }

    void saveAll() {
        org.transmartproject.db.dataquery.highdim.HighDimTestData.save patients
        org.transmartproject.db.dataquery.highdim.HighDimTestData.save patientTrials
    }

}
