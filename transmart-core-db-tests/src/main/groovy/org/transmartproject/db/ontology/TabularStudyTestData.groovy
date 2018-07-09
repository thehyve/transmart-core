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

package org.transmartproject.db.ontology

import org.transmartproject.db.StudyTestData
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.i2b2data.TrialVisit

import static org.transmartproject.db.TestDataHelper.save

class TabularStudyTestData {

    I2b2Data i2b2Data = I2b2Data.createDefault()

    ConceptTestData conceptData = ConceptTestData.createDefault()

    List<ObservationFact> facts = {
        Study dummyStudy = StudyTestData.createDefaultTabularStudy()
        TrialVisit dummyTrialVisit = ClinicalTestData.createTrialVisit("fake", 1, null, dummyStudy)
        ClinicalTestData.createTabularFacts(conceptData.conceptDimensions, i2b2Data.patients, dummyTrialVisit)
    }()

    List<I2b2> i2b2List = {
        conceptData.i2b2List
    }()

    TableAccess tableAccess = {
        conceptData.tableAccesses[0]
    }()

    List<ConceptDimension> concepts = {
        conceptData.conceptDimensions
    }()

    void saveAll() {
        i2b2Data.saveAll()
        conceptData.saveAll()
        save facts
    }

}
