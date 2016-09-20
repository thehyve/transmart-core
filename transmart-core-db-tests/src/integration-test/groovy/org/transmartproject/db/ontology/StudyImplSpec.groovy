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
import spock.lang.Specification

import static org.hamcrest.Matchers.containsInAnyOrder

@Integration
@Rollback

class StudyImplSpec extends Specification {

    StudyTestData studyTestData = new StudyTestData()

    StudiesResource studiesResourceService

    void setupData() {
        studyTestData.saveAll()
    }

    void testStudyGetAllPatients() {
        setupData()
        Study study = studiesResourceService.getStudyById('study_id_1')

        expect:
        study.patients containsInAnyOrder(studyTestData.i2b2Data.patients.collect { is it })
    }

    void testStudyGetName() {
        setupData()
        Study study = studiesResourceService.getStudyById('study_id_1')

        expect:
        study.id is('STUDY_ID_1' /* term name in uppercase */)
    }

}
