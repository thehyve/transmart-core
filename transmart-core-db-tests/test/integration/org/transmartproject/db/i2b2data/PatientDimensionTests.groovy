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
