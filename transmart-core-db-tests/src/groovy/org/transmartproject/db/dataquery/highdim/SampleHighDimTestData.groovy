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

package org.transmartproject.db.dataquery.highdim

import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.i2b2data.PatientTrialCoreDb

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.*
/**
 * Sample, generic high dimensional test data, not bound to any specific
 * data type.
 */
class SampleHighDimTestData {

    public static final String TRIAL_NAME = 'GENERIC_SAMPLE_TRIAL'

    DeGplInfo platform

    List<PatientDimension> patients

    List<PatientTrialCoreDb> patientTrialLinks

    List<DeSubjectSampleMapping> assays

    SampleHighDimTestData() {
        platform = new DeGplInfo(
                title: 'Test Generic Platform',
                organism: 'Homo Sapiens',
                markerType: 'generic',
                annotationDate: Date.parse('yyyy-MM-dd', '2013-05-03'),
                genomeReleaseId: 'hg18',
        )
        platform.id = 'test-generic-platform'

        patients =  I2b2Data.createTestPatients(2, -2000, TRIAL_NAME)

        patientTrialLinks = I2b2Data.createPatientTrialLinks(patients, TRIAL_NAME)

        assays = createTestAssays(patients, -3000L, platform, TRIAL_NAME)
    }

    void saveAll() {
        save([ platform ])
        save patients
        save patientTrialLinks
        save assays
    }

}
