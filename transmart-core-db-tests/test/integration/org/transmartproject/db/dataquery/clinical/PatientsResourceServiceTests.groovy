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
