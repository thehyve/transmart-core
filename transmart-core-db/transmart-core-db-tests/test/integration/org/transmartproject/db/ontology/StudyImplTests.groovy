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

import grails.test.mixin.TestMixin
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.is

@TestMixin(RuleBasedIntegrationTestMixin)
class StudyImplTests {

    StudyTestData studyTestData = new StudyTestData()

    StudiesResource studiesResourceService

    @Before
    void before() {
        studyTestData.saveAll()
    }

    @Test
    void testStudyGetAllPatients() {
        Study study = studiesResourceService.getStudyById('study_id_1')

        assertThat study.patients, containsInAnyOrder(studyTestData.i2b2Data.patients.collect { is it })
    }

    @Test
    void testStudyGetName() {
        Study study = studiesResourceService.getStudyById('study_id_1')

        assertThat study.id, is('STUDY_ID_1' /* term name in uppercase */)
    }

}
