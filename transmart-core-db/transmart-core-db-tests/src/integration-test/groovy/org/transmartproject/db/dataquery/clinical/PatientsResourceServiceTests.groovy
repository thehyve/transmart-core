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

package org.transmartproject.db.dataquery.clinical

import grails.test.mixin.TestMixin
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static groovy.util.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

@TestMixin(RuleBasedIntegrationTestMixin)
class PatientsResourceServiceTests {

    public static final String TRIAL_NAME = 'SAMPLE TRIAL'

    def patientsResourceService

    def sessionFactory

    def patients

    @Before
    void before() {
        patients = I2b2Data.createTestPatients(2, -100, TRIAL_NAME)
        save patients
        sessionFactory.currentSession.flush()
    }

    @Test
    void testLoadPatientById() {
        def result = patientsResourceService.getPatientById(-101L)

        assertThat result, is(patients[0])
    }

    @Test
    void testLoadNonExistentPatient() {
        shouldFail NoSuchResourceException, {
            patientsResourceService.getPatientById(-999999L)
        }
    }

}
