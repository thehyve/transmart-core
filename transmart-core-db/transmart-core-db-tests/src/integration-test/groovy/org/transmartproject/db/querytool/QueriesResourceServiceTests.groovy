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

package org.transmartproject.db.querytool

import grails.test.mixin.TestMixin
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.querytool.*
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin
import org.transmartproject.db.user.User

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.core.querytool.ConstraintByValue.Operator.*
import static org.transmartproject.core.querytool.ConstraintByValue.ValueType.FLAG
import static org.transmartproject.core.querytool.ConstraintByValue.ValueType.NUMBER
import static org.transmartproject.db.ontology.ConceptTestData.addI2b2
import static org.transmartproject.db.ontology.ConceptTestData.addTableAccess

@TestMixin(RuleBasedIntegrationTestMixin)
class QueriesResourceServiceTests {

    QueriesResource queriesResourceService
    def sessionFactory

    private void addObservationFact(Map extra,
                                    String conceptCd,
                                    BigDecimal patientNum) {
        def commonProperties = [
                encounterNum: 1,
                providerId:   1,
                startDate:    new Date(),
                modifierCd:   '@',
                instanceNum:  1,
        ]

        ObservationFact fact = new ObservationFact([
                *:commonProperties,
                *:extra,
                conceptCode:  conceptCd,
                patient: PatientDimension.load(patientNum),
        ])
        def ret = fact.save()
        assertThat(ret, is(notNullValue()))
    }

    private void addConceptDimension(String conceptCd, String conceptPath) {
        ConceptDimension conceptDimension = new ConceptDimension(
                conceptCode:   conceptCd,
                conceptPath: conceptPath
        )
        def ret = conceptDimension.save()
        assertThat(ret, is(notNullValue()))
    }

    private void addPatient(Long id) {
        Patient p = new PatientDimension()
        p.id = id
        assertThat p.save(), isA(Patient)
    }

    @Before
    void setUp() {
        /* 1. Define concepts */
        def concepts = [ /* level, concept_path, concept_cd */
                [0, "\\a\\",      'A'],
                [1, "\\a\\b\\", 'A:B'],
                [1, "\\a\\c\\", 'A:C'],
        ]
        addTableAccess(level: 0, fullName: '\\a\\', name: 'foo',
                tableCode: 'i2b2tc', tableName: 'i2b2')
        concepts.each {
            addI2b2(level              : it[0],
                    fullName           : it[1],
                    name               : it[2],
                    factTableColumn    : 'concept_cd',
                    dimensionTableName : 'concept_dimension',
                    columnName         : 'concept_path',
                    columnDataType     : 'T',
                    operator           : 'LIKE',
                    dimensionCode      : it[1])
            addConceptDimension(it[2], it[1])
        }

        /* 2. Create patients */
        (100..106).each {
            addPatient it
        }

        /* 3. Create facts */
        addObservationFact('A:B', 100, valueType: 'N', numberValue: 50, textValue: 'E')
        addObservationFact('A:C', 100, valueType: 'T', textValue: 'FOO')
        addObservationFact('A:B', 101, valueType: 'N', numberValue: 75, textValue: 'E')
        addObservationFact('A:B', 102, valueType: 'N', numberValue: 99, textValue: 'E')
        addObservationFact('A:B', 103, valueType: 'N', numberValue: 40, textValue: 'GE')
        addObservationFact('A:B', 104, valueFlag: 'L')
        addObservationFact('A:B', 105, valueType: 'N', textValue: 'BAR')
        addObservationFact('A:C', 105, valueType: 'N', numberValue: 40, textValue: 'L')
        addObservationFact('A:C', 106, valueType: 'N', textValue: 'XPTO')

        /* 4. Flush session so these objects are available from SQL */
        sessionFactory.currentSession.flush()
    }

    @Test
    void basicTest() {
        def definition = new QueryDefinition([
                new Panel(
                        invert: false,
                        items:  [
                                new Item(
                                        conceptKey: '\\\\i2b2tc\\a\\'
                                )
                        ]
                )
        ])

        def result = queriesResourceService.runQuery(definition)
        assertThat result, allOf(
                hasProperty("id", notNullValue()),
                hasProperty("setSize", equalTo(7L /* 100-106 */)),
                hasProperty("status", equalTo(QueryStatus.FINISHED)),
                hasProperty("errorMessage", nullValue()),
        )
    }

    private void assertPatientSet(QueryResult result, List patientNums) {
        assertThat result, hasProperty("setSize", equalTo(
                Long.valueOf(patientNums.size())))

        def patientSet = QtPatientSetCollection.where {
            eq('resultInstance.id', result.id)
        }.list()
        def memberMatchers = patientNums.collect {
            hasProperty('patient',
                    hasProperty('id', equalTo(Long.valueOf(it))))
        }
        assertThat patientSet, containsInAnyOrder(*memberMatchers)
    }

    @Test
    void testPanelInversion() {
        def definition = new QueryDefinition([
                new Panel(
                        items:  [
                                new Item(
                                        conceptKey: '\\\\i2b2tc\\a\\'
                                )
                        ]
                ),
                new Panel(
                        invert: true,
                        items: [
                                new Item(
                                        conceptKey: '\\\\i2b2tc\\a\\b\\'
                                )
                        ]
                )
        ])

        def result = queriesResourceService.runQuery(definition)
        assertPatientSet(result, [106])
    }

    @Test
    void testRegularNumberValueConstraint() {
        def definition = new QueryDefinition([
                new Panel(
                        items:  [
                                new Item(
                                        conceptKey: '\\\\i2b2tc\\a\\',
                                        constraint: new ConstraintByValue(
                                                valueType: NUMBER,
                                                operator: GREATER_OR_EQUAL_TO,
                                                constraint: '99'
                                        )
                                ),
                                new Item(
                                        conceptKey: '\\\\i2b2tc\\a\\',
                                        constraint: new ConstraintByValue(
                                                valueType: NUMBER,
                                                operator: LOWER_OR_EQUAL_TO,
                                                constraint: '40'
                                        )
                                )
                        ]
                )
        ])

        def result = queriesResourceService.runQuery(definition)
        assertPatientSet(result, [102, 105])
    }

    @Test
    void testMultiplePanelsIntersectAtPatientLevel() {
        def definition = new QueryDefinition([
                new Panel(
                        items:  [
                                new Item(
                                        conceptKey: '\\\\i2b2tc\\a\\b\\',
                                ),
                        ]
                ),
                new Panel(
                        items:  [
                                new Item(
                                        conceptKey: '\\\\i2b2tc\\a\\c\\',
                                ),
                        ]
                )
        ])

        def result = queriesResourceService.runQuery(definition)
        assertPatientSet(result, [100, 105])
    }

    @Test
    void testBetweenValueConstraint() {
        def definition = new QueryDefinition([
                new Panel(
                        items:  [
                                new Item(
                                        conceptKey: '\\\\i2b2tc\\a\\',
                                        constraint: new ConstraintByValue(
                                                valueType: NUMBER,
                                                operator: BETWEEN,
                                                constraint: '30 and 75'
                                        )
                                ),
                        ]
                )
        ])

        def result = queriesResourceService.runQuery(definition)
        assertPatientSet(result, [100, 101])
    }

    @Test
    void testFlagConstraint() {
        def definition = new QueryDefinition([
                new Panel(
                        items:  [
                                new Item(
                                        conceptKey: '\\\\i2b2tc\\a\\',
                                        constraint: new ConstraintByValue(
                                                valueType: FLAG,
                                                operator: EQUAL_TO,
                                                constraint: 'L'
                                        )
                                ),
                        ]
                )
        ])

        def result = queriesResourceService.runQuery(definition)
        assertPatientSet(result, [104])
    }

    @Test(expected=InvalidRequestException)
    void testUnknownItemKeyResultsInInvalidRequestException() {
        def definition = new QueryDefinition([
                new Panel(
                        items:  [
                                new Item(
                                        conceptKey: '\\\\bad table code\\a\\',
                                ),
                        ]
                )
        ])

        queriesResourceService.runQuery(definition)
    }

    @Test
    void runQueryAndFetchDefinitionAfterwards() {
        def inputDefinition = new QueryDefinition([
                new Panel(
                        items:  [
                                new Item(
                                        conceptKey: '\\\\i2b2tc\\a\\',
                                        constraint: new ConstraintByValue(
                                                valueType: NUMBER,
                                                operator: EQUAL_TO,
                                                constraint: '30.5'
                                        )
                                ),
                        ]
                )
        ])

        def result = queriesResourceService.runQuery(inputDefinition)
        assertThat result, hasProperty('id', is(notNullValue()))

        def outputDefinition = queriesResourceService
                .getQueryDefinitionForResult(result)

        assertThat outputDefinition, is(equalTo(inputDefinition))
    }

    @Test
    void testFailingQuery() {
        def inputDefinition = new QueryDefinition([])

        def orig = queriesResourceService.patientSetQueryBuilderService
        queriesResourceService.patientSetQueryBuilderService = [
                buildPatientSetQuery: {
                    QtQueryResultInstance resultInstance,
                    QueryDefinition definition,
                    User user = null ->
                    'fake query'
                }
        ] as PatientSetQueryBuilderService

        try {
            QueryResult result = queriesResourceService.runQuery(inputDefinition)

            assertThat result, hasProperty('status', equalTo(QueryStatus.ERROR))
        } finally {
            queriesResourceService.patientSetQueryBuilderService = orig
        }
    }


    @Test
    void testFailingQueryBuilding() {
        def inputDefinition = new QueryDefinition([])

        def orig = queriesResourceService.patientSetQueryBuilderService
        queriesResourceService.patientSetQueryBuilderService = [
                buildPatientSetQuery: {
                    QtQueryResultInstance resultInstance,
                    QueryDefinition definition,
                    User user = null ->
                        throw new RuntimeException('foo bar')
                }
        ] as PatientSetQueryBuilderService

        try {
        QueryResult result = queriesResourceService.runQuery(inputDefinition)

        assertThat result, hasProperty('status', equalTo(QueryStatus.ERROR))
        } finally {
            queriesResourceService.patientSetQueryBuilderService = orig
        }
    }

    @Test
    void testGetQueryResultFromIdBasic() {
        def queryMaster = QueryResultData.createQueryResult([
                PatientDimension.load(100) // see setUp()
        ])
        queryMaster.save(failOnError: true)
        QueryResult savedResultInstance =
                QueryResultData.getQueryResultFromMaster(queryMaster)

        def result = queriesResourceService.getQueryResultFromId(
                savedResultInstance.id)

        assertThat result, allOf(
                hasProperty('id', is(savedResultInstance.id)),
                hasProperty('setSize', is(1L) /* only patient #100 */),
        )
    }

    @Test
    void testQueryResultResultNonExistent() {
        shouldFail NoSuchResourceException, {
            queriesResourceService.getQueryResultFromId(-99112 /* bogus id */)
        }
    }

    @Test
    void testOverloadWithUsername() {
        def username = 'bogus_username'

        def definition = new QueryDefinition([
                new Panel(items: [
                        new Item(conceptKey: '\\\\i2b2tc\\a\\'),])])

        def result = queriesResourceService.runQuery(definition, username)

        assertThat result, hasProperty('username', is(username))
    }

}
