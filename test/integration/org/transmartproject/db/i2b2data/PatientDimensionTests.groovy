package org.transmartproject.db.i2b2data

import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.db.highdim.HighDimTestData

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@Mixin(HighDimTestData)
class PatientDimensionTests {

    @Before
    void setUp() {
        assertThat testRegionPatients*.save(), contains(
                isA(Patient), isA(Patient)
        )
    }

    @Test
    void testScalarPublicProperties() {
        /* Test properties defined in Patient */
        def patient = PatientDimension.get(testRegionPatients[0].id)

        assertThat patient, allOf(
                is(notNullValue(Patient)),
                hasProperty('id', equalTo(-2001L)),
                hasProperty('trial', equalTo('REGION_SAMP_TRIAL')),
                hasProperty('inTrialId', equalTo('SUBJ_ID_1')),
        )
    }

    @Test
    void testAssaysProperty() {
        testRegionPatients[1].assays = testRegionAssays
        testRegionPatients[1].assays = testRegionAssays.reverse()

        def patient = PatientDimension.get(testRegionPatients[1].id)

        assertThat patient, allOf(
                is(notNullValue(Patient)),
                hasProperty('assays', containsInAnyOrder(
                        allOf(
                                hasProperty('id', equalTo(-3002L)),
                                hasProperty('subjectId', equalTo('SUBJ_ID_2')),
                        ),
                        allOf(
                                hasProperty('id', equalTo(-3001L)),
                                hasProperty('subjectId', equalTo('SUBJ_ID_1')),
                        ),
                ))
        )
    }
}
