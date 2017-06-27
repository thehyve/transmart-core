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

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.TransmartSpecification

@Integration
@Rollback
class StudyImplSpec extends TransmartSpecification {

    // Making studyTestData @Lazy is a workaround to get this test to work on Travis Trusty environment. Probably it
    // is some bug in (some versions of) grails, but this is difficult to pin down.
    // Wrapping studyTestData in another class is because @Lazy doesn't work directly in Spock Specifications.
    TabularStudyTestData getStudyTestData() { holder.data }
    def holder = new Object() {
        @Lazy def data = new TabularStudyTestData()
    }

    StudiesResource studiesResourceService

    void setupData() {
        studyTestData.saveAll()
    }

    void testStudyGetAllPatients() {
        setupData()
        Study study = studiesResourceService.getStudyById('study_id_1')

        expect:
        (study.patients - studyTestData.i2b2Data.patients).empty
    }

    void testStudyGetName() {
        setupData()
        Study study = studiesResourceService.getStudyById('study_id_1')

        expect:
        study.id == 'STUDY_ID_1'
    }

}
