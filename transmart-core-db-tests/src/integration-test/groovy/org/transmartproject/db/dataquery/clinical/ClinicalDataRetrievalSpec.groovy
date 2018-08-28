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
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.clinical.ClinicalDataResource
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.core.dataquery.clinical.PatientRow
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.StudyTestData
import org.transmartproject.db.TestData
import org.transmartproject.db.i2b2data.TrialVisit
import spock.lang.Specification
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable
import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.ontology.ConceptTestData
import org.transmartproject.db.ontology.I2b2
import org.transmartproject.db.querytool.QtQueryMaster

import static org.hamcrest.Matchers.*
import static org.transmartproject.db.querytool.QueryResultData.createQueryResult
import static org.transmartproject.db.querytool.QueryResultData.getQueryResultFromMaster
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties

@Integration
@Rollback
class ClinicalDataRetrievalSpec extends Specification {

    @Autowired
    SessionFactory sessionFactory

    @Autowired
    ClinicalDataResource clinicalDataResource

    TestData testData

    TrialVisit trialVisit

    TabularResult<ClinicalVariableColumn, PatientRow> results

    I2b2 createI2b2(Map props) {
        ConceptTestData.createI2b2([code: props['name'], *: props])
    }

    TestData createTestData() {
        def tableAccess = ConceptTestData.createTableAccess(
                level: 0,
                fullName: '\\foo\\',
                name: 'foo',
                tableCode: 'i2b2 main',
                tableName: 'i2b2')

        def i2b2List = [
                createI2b2(level: 1, fullName: '\\foo\\concept 1\\', name: 'd1'), //not c, to test ordering
                createI2b2(level: 1, fullName: '\\foo\\concept 2\\', name: 'c2', cVisualattributes: 'LA'),
                createI2b2(level: 1, fullName: '\\foo\\concept 3\\', name: 'c3'),
                createI2b2(level: 1, fullName: '\\foo\\concept 4\\', name: 'c4'),
                createI2b2(level: 1, fullName: '\\foo\\concept 5\\', name: 'c5'),
                createI2b2(level: 1, fullName: '\\foo\\concept 6\\', name: 'c6'),
                createI2b2(level: 1, fullName: '\\foo\\concept 7\\', name: 'c7'),
        ]

        def conceptDims = ConceptTestData.createConceptDimensions(i2b2List)

        List<Patient> patients = I2b2Data.createTestPatients(3, -100, 'SAMP_TRIAL')

        def defaultStudy = StudyTestData.createDefaultTabularStudy()
        trialVisit = new TrialVisit(study: defaultStudy, relTimeUnit: 'week', relTime: 3, relTimeLabel: '3 weeks')

        def facts = ClinicalTestData.createTabularFacts(conceptDims, patients, trialVisit)

        def conceptData = new ConceptTestData(tableAccesses: [tableAccess], i2b2List: i2b2List, conceptDimensions: conceptDims)

        def i2b2Data = new I2b2Data(trialName: 'TEST', patients: patients)

        def clinicalData = new ClinicalTestData(
                patients: patients,
                facts: facts,
        )

        new TestData(conceptData: conceptData, i2b2Data: i2b2Data, clinicalData: clinicalData)
    }

    void setupData() {
        TestData.clearAllData()

        testData = createTestData()
        testData.saveAll()
    }

    void cleanup() {
        results?.close()
    }

    private String getConceptCodeFor(String conceptPath) {
        testData.conceptData.conceptDimensions.find {
            it.conceptCode == conceptPath
        }.conceptPath
    }

    void testColumnsLabel() {
        setupData()
        results = clinicalDataResource.retrieveData(testData.clinicalData.queryResult,
                [new TerminalConceptVariable(conceptCode: 'c2')])

        /* label for the columns (concepts) is the concept path */
        expect:
        results.indicesList[0].label == getConceptCodeFor('c2')
    }

    void testColumnIsClinicalVariable() {
        setupData()
        results = clinicalDataResource.retrieveData(testData.clinicalData.queryResult,
                [new TerminalConceptVariable(conceptCode: 'c2')])

        expect:
        results.indicesList.size() == 1
        results.indicesList[0] instanceof TerminalConceptVariable
        results.indicesList[0].conceptCode == 'c2'
        results.indicesList[0].conceptPath == getConceptCodeFor('c2')
    }

    void testRowsLabel() {
        setupData()
        results = clinicalDataResource.retrieveData(testData.clinicalData.queryResult,
                [new TerminalConceptVariable(conceptCode: 'c2')])
        def expected = testData.i2b2Data.patients.sort {
            it.id
        }.collect {
            it.inTrialId
        }
        /* Label for the rows is the patients' inTrialId.
         * Patients are sorted by id */
        expect:
        results*.label == expected
    }

    void testMultipleQueryResultsVariant() {
        setupData()
        results = clinicalDataResource.retrieveData(
                testData.i2b2Data.patients[0..1].collect {
                    QtQueryMaster result = createQueryResult('clinical-patient-set', [it])
                    result.save()
                    getQueryResultFromMaster(result)
                },
                [new TerminalConceptVariable(conceptCode: 'c2')])
        def expected = testData.i2b2Data.patients[0..1].
                sort { it.id }.
                collect { it.id }

        expect:
        results.collect { it.patient.id } == expected
    }

    void testPatientCanBeFoundInRow() {
        setupData()
        results = clinicalDataResource.retrieveData(testData.clinicalData.queryResult,
                [new TerminalConceptVariable(conceptCode: 'c2')])

        List<PatientRow> rows = Lists.newArrayList results

        expect:
        rows allOf(
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

    void testDataStringDataPoints() {
        setupData()
        results = clinicalDataResource.retrieveData(testData.clinicalData.queryResult,
                [new TerminalConceptVariable(conceptCode: 'c2')])

        List<PatientRow> rows = Lists.newArrayList results

        expect:
        rows contains(
                testData.clinicalData.facts.sort {
                    it.patientId
                }.findAll {
                    it.conceptCode == 'c2'
                }.collect {
                    contains it.textValue
                })
    }

    void testMissingData() {
        setupData()
        /* test for when no data whatsoever is returned */

        results = clinicalDataResource.retrieveData(testData.clinicalData.queryResult,
                [new TerminalConceptVariable(conceptCode: 'c4')])

        List<PatientRow> rows = Lists.newArrayList results

        expect:
        rows contains(
                [contains(nullValue())] * 3
        )
    }

    void testMissingColumnValue() {
        setupData()
        /* test when a row has data for some but not all variables */
        results = clinicalDataResource.retrieveData(testData.clinicalData.queryResult,
                [new TerminalConceptVariable(conceptCode: 'c3')])

        List<PatientRow> rows = Lists.newArrayList results

        expect:
        rows contains(
                /* see test data */
                contains(isA(Number) /* -45.42 */),
                contains(equalTo('')),
                contains(nullValue()))
    }

    void testNumericDataIsInNumericForm() {
        setupData()
        results = clinicalDataResource.retrieveData(testData.clinicalData.queryResult,
                [new TerminalConceptVariable(conceptCode: 'c3')])
        List<PatientRow> rows = Lists.newArrayList results

        expect:
        rows hasItem(allOf(
                /* see test data */
                hasProperty('patient',
                        hasSameInterfaceProperties(Patient,
                                testData.i2b2Data.patients[2] /* -103 */, ['assays'])),
                /* numberValue prop in ObservationFact has scale 16 */
                contains(equalTo(-45.4200000000000000 /* big decimal */))))
    }


    void testWithConceptSpecifiedByPath() {
        setupData()
        def expected = testData.clinicalData.facts.sort {
            it.patientId
        }.findAll {
            it.conceptCode == 'c2'
        }.collect {
            it.textValue
        }

        when: 'test when a row has data for some but not all variables'
        def resultList = Lists.newArrayList(clinicalDataResource.retrieveData(testData.clinicalData.queryResult,
                [new TerminalConceptVariable(conceptPath: '\\foo\\concept 2\\')]))
        then:
        resultList.collect { it.collect() }.flatten() == expected
    }

    void testColumnsAndDataAreInTheSpecifiedOrder() {
        setupData()
        def conceptVariables = [
                new TerminalConceptVariable(conceptCode: 'd1'),
                new TerminalConceptVariable(conceptCode: 'c2')]
        def expectedOrder = ['d1', 'c2']

        def expected = testData.clinicalData.facts.sort { ObservationFact fact1,
                                                          ObservationFact fact2 ->
            fact1.patientId <=> fact2.patientId ?:
                    expectedOrder.indexOf(fact1.conceptCode) <=>
                            expectedOrder.indexOf(fact2.conceptCode)
        }.findAll {
            it.conceptCode == 'c2' || it.conceptCode == 'd1'
        }.groupBy {
            it.patientId
        }.collect { patientId, List<ObservationFact> facts ->
            facts*.textValue
        }


        when:
        def resultList = Lists.newArrayList(clinicalDataResource.retrieveData(
                testData.clinicalData.queryResult, conceptVariables))
        then:
        resultList.collect { it.collect() } == expected

        when: 'with variables reversed'
        conceptVariables = conceptVariables.reverse()
        def resultListReversed = Lists.newArrayList(clinicalDataResource.retrieveData(
                testData.clinicalData.queryResult, conceptVariables))

        then:
        resultListReversed.collect { it.collect() } == expected.collect { it.reverse() }
    }


    void testRepeatedDataPoint() {
        setupData()
        ClinicalTestData.createObservationFact(
                testData.conceptData.conceptDimensions.find { it.conceptCode == 'c2' },
                testData.i2b2Data.patients[1],
                -20000,
                'foobar',
                trialVisit
        ).save(failOnError: true)
        sessionFactory.currentSession.flush()

        when:
        results = clinicalDataResource.retrieveData(
                testData.clinicalData.queryResult,
                [new TerminalConceptVariable(conceptCode: 'c2')])
        Lists.newArrayList results /* consume the data */
        then:
        def e = thrown(UnexpectedResultException)
        e.message.contains('Got more than one fact')
    }


    void testInexistentConcept() {
        setupData()

        when:
        clinicalDataResource.retrieveData(testData.clinicalData.queryResult,
                [new TerminalConceptVariable(conceptCode: 'non_existent')])
        then:
        def e = thrown(InvalidArgumentsException)
        e.message.contains('Concept code')
        e.message.contains('did not yield any results')
    }

    void testRepeatedConceptCode() {
        setupData()
        when:
        clinicalDataResource.retrieveData(testData.clinicalData.queryResult,
                [new TerminalConceptVariable(conceptCode: 'c2'),
                 new TerminalConceptVariable(conceptCode: 'c4'),
                 new TerminalConceptVariable(conceptCode: 'c2')])
        then:
        def e = thrown(InvalidArgumentsException)
        e.message.contains('same concept code')
    }

    void testRepeatedConceptPath() {
        setupData()

        when:
        clinicalDataResource.retrieveData(testData.clinicalData.queryResult,
                [new TerminalConceptVariable(conceptPath: getConceptCodeFor('c2')),
                 new TerminalConceptVariable(conceptPath: getConceptCodeFor('c2'))])
        then:
        def e = thrown(InvalidArgumentsException)
        e.message.contains('same concept path')
    }


    void testMixedRepetition() {
        setupData()

        when:
        clinicalDataResource.retrieveData(testData.clinicalData.queryResult,
                [new TerminalConceptVariable(conceptCode: 'c2'),
                 new TerminalConceptVariable(conceptPath: getConceptCodeFor('c2'))])
        then:
        def e = thrown(InvalidArgumentsException)
        e.message.contains('Repeated variables in the query')
    }

    void testRetrieveDataWithPatientsDirectly() {
        setupData()
        def patientIds = [-101L, -102L]
        def conceptCode = 'c2'
        Set patients = testData.i2b2Data.patients.findAll {
            it.id in patientIds
        }

        results = clinicalDataResource.retrieveData(patients, [
                new TerminalConceptVariable(conceptCode: conceptCode)])
        List<PatientRow> rows = Lists.newArrayList(results)
        def expected = testData.clinicalData.facts.sort {
            it.patientId
        }.findAll {
            it.conceptCode == conceptCode &&
                    it.patient.id in patientIds
        }.collect {
            it.textValue
        }
        expect:
        rows.collect { it.collect() }.flatten() == expected
    }

    void testRetrieveDataWithoutVariables() {
        setupData()

        when:
        clinicalDataResource.retrieveData(
                testData.i2b2Data.patients as Set, [])
        then:
        thrown(InvalidArgumentsException)
    }

    void testRetrieveDataWithoutPatientsVariantQueryResult() {
        setupData()
        results = clinicalDataResource.retrieveData(emptyQueryResult, [
                new TerminalConceptVariable(conceptCode: 'c2')])

        expect:
        results.size() == 0
    }

    void testRetrieveDataNullQueryResultVariantQueryResult() {
        setupData()

        when:
        clinicalDataResource.retrieveData((QueryResult) null, [
                new TerminalConceptVariable(conceptCode: 'c2')])
        then:
        thrown(AssertionError)
    }

    void testRetrieveDataWithoutPatientsVariantQueryResultList() {
        setupData()
        results = clinicalDataResource.retrieveData([] as Set, [
                new TerminalConceptVariable(conceptCode: 'c2')])

        expect:
        results.size() == 0
    }

    void testRetrieveDataWithoutPatientsVariantSet() {
        setupData()
        results = clinicalDataResource.retrieveData([] as Set, [
                new TerminalConceptVariable(conceptCode: 'c2')])

        expect:
        results.size() == 0
    }

    QueryResult getEmptyQueryResult() {
        def result = createQueryResult('clinical-test-set', [])
        result.save(flush: true)
        getQueryResultFromMaster(result)
    }

}
