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

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.util.logging.Slf4j
import spock.lang.Specification

import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.db.i2b2data.I2b2Data

import static groovy.util.GroovyAssert.shouldFail
import static org.hamcrest.Matchers.is
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

@Integration
@Rollback
@Slf4j
class PatientsResourceServiceSpec extends Specification {

    public static final String TRIAL_NAME = 'SAMPLE TRIAL'

    def patientsResourceService

    def sessionFactory

    def patients

    void before() {
        patients = I2b2Data.createTestPatients(2, -100, TRIAL_NAME)
        save patients
        sessionFactory.currentSession.flush()
    }

    void testLoadPatientById() {
        def result = patientsResourceService.getPatientById(-101L)

        expect: result is(patients[0])
    }

    void testLoadNonExistentPatient() {
        shouldFail NoSuchResourceException, {
            patientsResourceService.getPatientById(-999999L)
        }
    }

}
