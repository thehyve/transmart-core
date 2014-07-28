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

package org.transmartproject.db

import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.dataquery.highdim.SampleBioMarkerTestData
import org.transmartproject.db.dataquery.highdim.acgh.AcghTestData
import org.transmartproject.db.dataquery.highdim.mrna.MrnaTestData
import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.ontology.ConceptTestData

class TestData {

    ConceptTestData conceptData
    I2b2Data i2b2Data
    ClinicalTestData clinicalData
    MrnaTestData mrnaData
    AcghTestData acghData
    SampleBioMarkerTestData bioMarkerTestData

    static TestData createDefault() {
        def conceptData = ConceptTestData.createDefault()
        def i2b2Data = I2b2Data.createDefault()
        def clinicalData = ClinicalTestData.createDefault(conceptData.i2b2List, i2b2Data.patients)

        def bioMarkerTestData = new SampleBioMarkerTestData()
        def mrnaData = new MrnaTestData('2', bioMarkerTestData) //concept code '2'
        def acghData = new AcghTestData('4', bioMarkerTestData) //concept code '4'

        new TestData(
                conceptData: conceptData,
                i2b2Data: i2b2Data,
                clinicalData: clinicalData,
                mrnaData:  mrnaData,
                acghData: acghData,
                bioMarkerTestData: bioMarkerTestData,
        )
    }

    void saveAll() {
        conceptData?.saveAll()
        i2b2Data?.saveAll()
        clinicalData?.saveAll()
        bioMarkerTestData?.saveAll()
        mrnaData?.saveAll()
        mrnaData?.updateDoubleScaledValues()
        acghData?.saveAll()
    }
}
