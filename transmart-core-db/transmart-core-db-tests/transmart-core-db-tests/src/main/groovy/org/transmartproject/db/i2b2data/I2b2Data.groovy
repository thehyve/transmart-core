/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.i2b2data

import static org.transmartproject.db.TestDataHelper.save

class I2b2Data {

    final static String DEFAULT_TRIAL_NAME = 'STUDY_ID_1'
    String trialName
    List<PatientDimension> patients
    List<PatientTrialCoreDb> patientTrials

    static I2b2Data createDefault() {
        List<PatientDimension> patients = createTestPatients(3, -100, DEFAULT_TRIAL_NAME)
        List<PatientTrialCoreDb> patientTrials = createPatientTrialLinks(patients, DEFAULT_TRIAL_NAME)
        new I2b2Data(trialName: DEFAULT_TRIAL_NAME, patients: patients, patientTrials: patientTrials)
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
