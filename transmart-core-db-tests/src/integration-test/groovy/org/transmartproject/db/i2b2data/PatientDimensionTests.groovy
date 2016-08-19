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

import grails.test.mixin.TestMixin
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.db.dataquery.highdim.SampleHighDimTestData
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestMixin(RuleBasedIntegrationTestMixin)
class PatientDimensionTests {

    SampleHighDimTestData testData = new SampleHighDimTestData()

    @Before
    void setUp() {
        testData.saveAll()
    }

    @Test
    void testScalarPublicProperties() {
        /* Test properties defined in Patient */
        def patient = PatientDimension.get(testData.patients[0].id)

        assertThat patient, allOf(
                is(notNullValue(Patient)),
                hasProperty('id', equalTo(-2001L)),
                hasProperty('trial', equalTo(testData.TRIAL_NAME)),
                hasProperty('inTrialId', equalTo('SUBJ_ID_1')),
        )
    }

    @Test
    void testAssaysProperty() {
        testData.patients[1].assays = testData.assays
        testData.patients[1].assays = testData.assays.reverse()

        def patient = PatientDimension.get(testData.patients[1].id)

        assertThat patient, allOf(
                is(notNullValue(Patient)),
                hasProperty('assays', containsInAnyOrder(
                        allOf(
                                hasProperty('id', equalTo(-3002L)),
                                hasProperty('patientInTrialId', equalTo('SUBJ_ID_2')),
                        ),
                        allOf(
                                hasProperty('id', equalTo(-3001L)),
                                hasProperty('patientInTrialId', equalTo('SUBJ_ID_1')),
                        ),
                ))
        )
    }

}
