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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.db.dataquery.highdim.SampleHighDimTestData
import spock.lang.Specification

import static org.hamcrest.Matchers.*

@Integration
@Rollback
class PatientDimensionSpec extends Specification {

    SampleHighDimTestData testData

    void setupData() {
        testData = new SampleHighDimTestData()
        testData.saveAll()
    }

    void "test scalar public properties"() {
        given:
        setupData()

        /* Test properties defined in Patient */
        def patient = PatientDimension.get(testData.patients[0].id)

        expect:
        patient allOf(
                is(notNullValue(Patient)),
                hasProperty('id', equalTo(-2001L)),
                hasProperty('trial', equalTo(testData.TRIAL_NAME)),
                hasProperty('inTrialId', equalTo('SUBJ_ID_1')),
        )
    }

    void "test assays property"() {
        given:
        setupData()

        def patient1 = testData.patients[1]
        patient1.assays = testData.assays

        def patient = PatientDimension.get(patient1.id)

        expect:
        patient allOf(
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
