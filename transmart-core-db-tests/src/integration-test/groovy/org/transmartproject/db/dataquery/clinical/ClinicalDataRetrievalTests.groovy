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

import com.google.common.collect.Lists
import grails.test.mixin.TestMixin
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.core.dataquery.clinical.PatientRow
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.TestData
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable
import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.ontology.ConceptTestData
import org.transmartproject.db.ontology.I2b2
import org.transmartproject.db.querytool.QtQueryMaster
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.db.querytool.QueryResultData.createQueryResult
import static org.transmartproject.db.querytool.QueryResultData.getQueryResultFromMaster
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties

@TestMixin(RuleBasedIntegrationTestMixin)
class ClinicalDataRetrievalTests {

    TestData testData

    def clinicalDataResourceService

    def sessionFactory

    TabularResult<ClinicalVariableColumn, PatientRow> results

    I2b2 createI2b2(Map props) {
        ConceptTestData.createI2b2([code: props['name'], *: props])
    }

    TestData createTestData() {

        def tableAccess = ConceptTestData.createTableAccess(
                level:     0,
                fullName:  '\\foo\\',
                name:      'foo',
                tableCode: 'i2b2 main',
                tableName: 'i2b2')

        def i2b2List = [
                createI2b2(level: 1, fullName: '\\foo\\concept 1\\', name: 'd1'), //not c, to test ordering
                createI2b2(level: 1, fullName: '\\foo\\concept 2\\', name: 'c2', cVisualattributes: 'LA'),
                createI2b2(level: 1, fullName: '\\foo\\concept 3\\', name: 'c3'),
                createI2b2(level: 1, fullName: '\\foo\\concept 4\\', name: 'c4'),
        ]

        def conceptDims = ConceptTestData.createConceptDimensions(i2b2List)

        List<Patient> patients =  I2b2Data.createTestPatients(3, -100, 'SAMP_TRIAL')

        def facts = ClinicalTestData.createFacts(conceptDims, patients)

        def conceptData = new ConceptTestData(tableAccesses: [tableAccess], i2b2List: i2b2List, conceptDimensions: conceptDims)

        def i2b2Data = new I2b2Data(trialName: 'TEST', patients: patients)

        def clinicalData = new ClinicalTestData(patients: patients, facts: facts)

        new TestData(conceptData: conceptData, i2b2Data: i2b2Data, clinicalData: clinicalData)
    }

    @Before
    void setUp() {
        testData = createTestData()
        testData.saveAll()
        sessionFactory.currentSession.flush()
    }

    @After
    void tearDown() {
        results?.close()
    }

    private String getConceptCodeFor(String conceptPath) {
        testData.conceptData.conceptDimensions.find {
            it.conceptCode == conceptPath
        }.conceptPath
    }

    @Test
    void testColumnsLabel() {
        results = clinicalDataResourceService.retrieveData(testData.clinicalData.queryResult,
                [ new TerminalConceptVariable(conceptCode: 'c2') ])

        /* label for the columns (concepts) is the concept path */
        assertThat results, hasProperty('indicesList',
                contains(allOf(
                        hasProperty('label', equalTo(
                                getConceptCodeFor('c2')
                        ))
                )))
    }

    @Test
    void testColumnIsClinicalVariable() {
        results = clinicalDataResourceService.retrieveData(testData.clinicalData.queryResult,
                [ new TerminalConceptVariable(conceptCode: 'c2') ])

        assertThat results, hasProperty('indicesList',
                contains(allOf(
                        isA(TerminalConceptVariable),
                        hasProperty('conceptCode', is('c2')),
                        hasProperty('conceptPath', is(
                                getConceptCodeFor('c2'))))))
    }

    @Test
    void testRowsLabel() {
        results = clinicalDataResourceService.retrieveData(testData.clinicalData.queryResult,
                [ new TerminalConceptVariable(conceptCode: 'c2') ])

        /* Label for the rows is the patients' inTrialId.
         * Patients are sorted by id */
        assertThat results, contains(
                testData.i2b2Data.patients.sort {
                    it.id
                }.collect {
                    hasProperty('label', is(it.inTrialId))
                })
    }

    @Test
    void testMultipleQueryResultsVariant() {
        results = clinicalDataResourceService.retrieveData(
                testData.i2b2Data.patients[0..1].collect {
                    QtQueryMaster result = createQueryResult([it])
                    result.save()
                    getQueryResultFromMaster(result)
                },
                [ new TerminalConceptVariable(conceptCode: 'c2') ])

        assertThat results, contains(
                testData.i2b2Data.patients[0..1].
                        sort { it.id }.
                        collect {
                            hasProperty('patient', hasProperty('id', is(it.id)))
                        })
    }

    @Test
    void testPatientCanBeFoundInRow() {
        results = clinicalDataResourceService.retrieveData(testData.clinicalData.queryResult,
                [ new TerminalConceptVariable(conceptCode: 'c2') ])

        List<PatientRow> rows = Lists.newArrayList results

        assertThat rows, allOf(
                everyItem(isA(PatientRow)),
                contains(
                        testData.i2b2Data.patients.sort {
                            it.id
                        }.collect {
                            hasProperty('patient',
                                    hasSameInterfaceProperties(Patient, it, ['assays']))
                        })
        )
    }

    @Test
    void testDataStringDataPoints() {
        results = clinicalDataResourceService.retrieveData(testData.clinicalData.queryResult,
                [ new TerminalConceptVariable(conceptCode: 'c2') ])

        List<PatientRow> rows = Lists.newArrayList results

        assertThat rows, contains(
                testData.clinicalData.facts.sort {
                    it.patientId
                }.findAll {
                    it.conceptCode == 'c2'
                }.collect {
                    contains it.textValue
                })
    }

    @Test
    void testMissingData() {
        /* test for when no data whatsoever is returned */

        results = clinicalDataResourceService.retrieveData(testData.clinicalData.queryResult,
                [ new TerminalConceptVariable(conceptCode: 'c4') ])

        List<PatientRow> rows = Lists.newArrayList results

        assertThat rows, contains(
                [ contains(nullValue()) ] * 3
        )
    }

    @Test
    void testMissingColumnValue() {
        /* test when a row has data for some but not all variables */
        results = clinicalDataResourceService.retrieveData(testData.clinicalData.queryResult,
                [ new TerminalConceptVariable(conceptCode: 'c3') ])

        List<PatientRow> rows = Lists.newArrayList results

        assertThat rows, contains(
                /* see test data */
                contains(isA(Number) /* -45.42 */),
                contains(equalTo('')),
                contains(nullValue()))
    }

    @Test
    void testNumericDataIsInNumericForm() {
        results = clinicalDataResourceService.retrieveData(testData.clinicalData.queryResult,
                [ new TerminalConceptVariable(conceptCode: 'c3') ])
        List<PatientRow> rows = Lists.newArrayList results

        assertThat rows, hasItem(allOf(
                /* see test data */
                hasProperty('patient',
                        hasSameInterfaceProperties(Patient,
                                testData.i2b2Data.patients[2] /* -103 */, ['assays'])),
                /* numberValue prop in ObservationFact has scale 5 */
                contains(equalTo(-45.42000 /* big decimal */))))
    }


    @Test
    void testWithConceptSpecifiedByPath() {
        /* test when a row has data for some but not all variables */
        results = clinicalDataResourceService.retrieveData(testData.clinicalData.queryResult,
                [ new TerminalConceptVariable(conceptPath: '\\foo\\concept 2\\') ])

        assertThat results, contains(
                testData.clinicalData.facts.sort {
                    it.patientId
                }.findAll {
                    it.conceptCode == 'c2'
                }.collect {
                    contains it.textValue
                })
    }

    @Test
    void testColumnsAndDataAreInTheSpecifiedOrder() {
        def conceptVariables =  [
                new TerminalConceptVariable(conceptCode: 'd1'),
                new TerminalConceptVariable(conceptCode: 'c2') ]
        results = clinicalDataResourceService.retrieveData(
                testData.clinicalData.queryResult, conceptVariables)

        def expectedOrder = [ 'd1', 'c2' ]

        def createMatcher = { ->
            contains(
                    testData.clinicalData.facts.sort { ObservationFact fact1,
                                          ObservationFact fact2 ->
                        fact1.patientId <=> fact2.patientId ?:
                                expectedOrder.indexOf(fact1.conceptCode) <=>
                                        expectedOrder.indexOf(fact2.conceptCode)
                    }.findAll {
                        it.conceptCode == 'c2' || it.conceptCode == 'd1'
                    }.groupBy {
                        it.patientId
                    }.collect { patientId, List<ObservationFact> facts ->
                        contains(facts*.textValue.collect { is it })
                    })
        }

        def resultList = Lists.newArrayList(results)
        assertThat resultList, createMatcher()
        results.close()

        /* now with variables reversed */
        conceptVariables = conceptVariables.reverse()

        results = clinicalDataResourceService.retrieveData(
                testData.clinicalData.queryResult, conceptVariables)

        expectedOrder = expectedOrder.reverse()

        assertThat Lists.newArrayList(results), createMatcher()
    }


    @Test
    void testRepeatedDataPoint() {
        ClinicalTestData.createObservationFact(
                testData.conceptData.conceptDimensions.find { it.conceptCode == 'c2' },
                testData.i2b2Data.patients[1],
                -20000,
                'foobar').save(failOnError: true)
        sessionFactory.currentSession.flush()

        def exception = shouldFail UnexpectedResultException, {
            results = clinicalDataResourceService.retrieveData(
                    testData.clinicalData.queryResult,
                    [ new TerminalConceptVariable(conceptCode: 'c2') ])
            def res = Lists.newArrayList results /* consume the data */
            println res
        }

        assertThat exception, hasProperty('message',
                containsString('Got more than one fact'))
    }


    @Test
    void testInexistentConcept() {

        assertThat shouldFail(InvalidArgumentsException, {
            clinicalDataResourceService.retrieveData(testData.clinicalData.queryResult,
                    [ new TerminalConceptVariable(conceptCode: 'non_existent') ])
        }), hasProperty('message', allOf(
                containsString('Concept code'),
                containsString('did not yield any results')))
    }

    @Test
    void testRepeatedConceptCode() {
        assertThat shouldFail(InvalidArgumentsException, {
            clinicalDataResourceService.retrieveData(testData.clinicalData.queryResult,
                    [ new TerminalConceptVariable(conceptCode: 'c2'),
                            new TerminalConceptVariable(conceptCode: 'c4'),
                            new TerminalConceptVariable(conceptCode: 'c2') ])
        }), hasProperty('message', containsString('same concept code'))
    }

    @Test
    void testRepeatedConceptPath() {
        assertThat shouldFail(InvalidArgumentsException, {
            clinicalDataResourceService.retrieveData(testData.clinicalData.queryResult,
                    [ new TerminalConceptVariable(conceptPath: getConceptCodeFor('c2')),
                            new TerminalConceptVariable(conceptPath: getConceptCodeFor('c2'))])
        }), hasProperty('message', containsString('same concept path'))
    }


    @Test
    void testMixedRepetition() {
        assertThat shouldFail(InvalidArgumentsException, {
            clinicalDataResourceService.retrieveData(testData.clinicalData.queryResult,
                    [ new TerminalConceptVariable(conceptCode: 'c2'),
                            new TerminalConceptVariable(conceptPath: getConceptCodeFor('c2'))])
        }), hasProperty('message', containsString('Repeated variables in the query'))
    }

    @Test
    void testRetrieveDataWithPatientsDirectly() {
        def patientIds = [-101L, -102L]
        def conceptCode = 'c2'
        Set patients = testData.i2b2Data.patients.findAll {
            it.id in patientIds
        }

        results = clinicalDataResourceService.retrieveData(patients, [
                new TerminalConceptVariable(conceptCode: conceptCode)])
        List<PatientRow> rows = Lists.newArrayList(results)

        assertThat rows, contains(
                testData.clinicalData.facts.sort {
                    it.patientId
                }.findAll {
                    it.conceptCode == conceptCode &&
                            it.patient.id in patientIds
                }.collect {
                    contains it.textValue
                }
        )
    }

    @Test
    void testRetrieveDataWithoutVariables() {
        shouldFail InvalidArgumentsException, {
            clinicalDataResourceService.retrieveData(
                    testData.i2b2Data.patients as Set, [])
        }
    }

    @Test
    void testRetrieveDataWithoutPatientsVariantQueryResult() {
        results = clinicalDataResourceService.retrieveData(emptyQueryResult, [
                new TerminalConceptVariable(conceptCode: 'c2')])

        assertThat results, is(iterableWithSize(0))
    }

    @Test
    void testRetrieveDataNullQueryResultVariantQueryResult() {
        shouldFail AssertionError, {
            clinicalDataResourceService.retrieveData((QueryResult) null, [
                new TerminalConceptVariable(conceptCode: 'c2')])
        }
    }

    @Test
    void testRetrieveDataWithoutPatientsVariantQueryResultList() {
        results = clinicalDataResourceService.retrieveData([] as Set, [
                new TerminalConceptVariable(conceptCode: 'c2')])

        assertThat results, is(iterableWithSize(0))
    }

    @Test
    void testRetrieveDataWithoutPatientsVariantSet() {
        results = clinicalDataResourceService.retrieveData([] as Set, [
                new TerminalConceptVariable(conceptCode: 'c2')])

        assertThat results, is(iterableWithSize(0))
    }

    QueryResult getEmptyQueryResult() {
        def result = createQueryResult([])
        result.save(flush: true)
        getQueryResultFromMaster(result)
    }

}
