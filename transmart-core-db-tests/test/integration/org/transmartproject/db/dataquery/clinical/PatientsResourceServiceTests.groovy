package org.transmartproject.db.dataquery.clinical

import grails.test.mixin.TestMixin
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.ontology.ConceptTestData
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

    def concepts

    def observations

    @Before
    void before() {
        patients = I2b2Data.createTestPatients(5, -100, TRIAL_NAME)
        concepts = ConceptTestData.createMultipleI2B2(3)
        observations = createObservations()

        ConceptTestData.addTableAccess(level: 0, fullName: '\\test\\', name: 'test',
                tableCode: 'i2b2 main', tableName: 'i2b2')
        save patients
        save concepts
        save observations
        save ConceptTestData.createConceptDimensions(concepts)

        sessionFactory.currentSession.flush()
    }

    def createObservations() {
        List result = []
        //concept 0 with patients 0, 1 and 2
        result << ClinicalTestData.createObservationFact(concepts[0].code, patients[0], 1, 2)
        result << ClinicalTestData.createObservationFact(concepts[0].code, patients[1], 1, 2)
        result << ClinicalTestData.createObservationFact(concepts[0].code, patients[2], 1, 2)
        //concept 1 with patients 2 and 3
        result << ClinicalTestData.createObservationFact(concepts[1].code, patients[2], 1, 2)
        result << ClinicalTestData.createObservationFact(concepts[1].code, patients[3], 1, 2)
        result
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

    @Test
    void testLoadPatientsByConcept() {
        def result = patientsResourceService.getPatients(concepts[0])
        def expected = new HashSet()
        expected.add(patients[0])
        expected.add(patients[1])
        expected.add(patients[2])

        assertThat new HashSet(result), is(expected)

        def result2 = patientsResourceService.getPatients(concepts[1])
        def expected2 = new HashSet()
        expected2.add(patients[2])
        expected2.add(patients[3])

        assertThat new HashSet(result2), is(expected2)

        def result3 = patientsResourceService.getPatients(concepts[2])
        assertThat result3.size(), is(0)
    }

}