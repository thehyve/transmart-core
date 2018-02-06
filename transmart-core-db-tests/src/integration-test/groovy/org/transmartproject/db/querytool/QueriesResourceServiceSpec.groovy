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

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.querytool.*
import org.transmartproject.db.TestData
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.i2b2data.TrialVisit
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.user.User
import org.transmartproject.db.TransmartSpecification

import static org.hamcrest.Matchers.*
import static org.transmartproject.core.querytool.ConstraintByValue.Operator.*
import static org.transmartproject.core.querytool.ConstraintByValue.ValueType.FLAG
import static org.transmartproject.core.querytool.ConstraintByValue.ValueType.NUMBER
import static org.transmartproject.db.ontology.ConceptTestData.addI2b2
import static org.transmartproject.db.ontology.ConceptTestData.addTableAccess

@Integration
@Rollback
class QueriesResourceServiceSpec extends TransmartSpecification {

    QueriesResource queriesResourceService
    def sessionFactory

    private void addObservationFact(Map extra,
                                    String conceptCd,
                                    BigDecimal patientNum) {
        def commonProperties = [
                encounterNum: -1,
                providerId  : 1,
                startDate   : new Date(),
                modifierCd  : '@',
                instanceNum : 1,
        ]

        ObservationFact fact = new ObservationFact([
                *          : commonProperties,
                *          : extra,
                conceptCode: conceptCd,
                patient    : PatientDimension.load(patientNum),
        ])
        assert fact.save()
    }

    private void addConceptDimension(String conceptCd, String conceptPath) {
        ConceptDimension conceptDimension = new ConceptDimension(
                conceptCode: conceptCd,
                conceptPath: conceptPath
        )
        assert conceptDimension.save()
    }

    private void addPatient(Long id) {
        Patient p = new PatientDimension()
        p.id = id
        assert p.save()
    }

    void setupData() {
        TestData.clearAllData()

        /* 1. Define concepts */
        def concepts = [ /* level, concept_path, concept_cd */
                         [0, "\\a\\", 'A'],
                         [1, "\\a\\b\\", 'A:B'],
                         [1, "\\a\\c\\", 'A:C'],
        ]
        addTableAccess(level: 0, fullName: '\\a\\', name: 'foo',
                tableCode: 'i2b2tc', tableName: 'i2b2')
        concepts.each {
            addI2b2(level: it[0],
                    fullName: it[1],
                    name: it[2],
                    factTableColumn: 'concept_cd',
                    dimensionTableName: 'concept_dimension',
                    columnName: 'concept_path',
                    columnDataType: 'T',
                    operator: 'LIKE',
                    dimensionCode: it[1])
            addConceptDimension(it[2], it[1])
        }

        /* 2. Create patients */
        (100..106).each {
            addPatient it
        }

        def study = new Study(
                studyId: "study_name",
                secureObjectToken: "EXP:study_name"
        )

        def trialVisit = new TrialVisit(
                relTimeUnit: "days",
                relTime: 3,
                study: study
        )

        /* 3. Create facts */
        addObservationFact('A:B', 100, valueType: 'N', numberValue: 50, textValue: 'E', trialVisit: trialVisit)
        addObservationFact('A:C', 100, valueType: 'T', textValue: 'FOO', trialVisit: trialVisit)
        addObservationFact('A:B', 101, valueType: 'N', numberValue: 75, textValue: 'E', trialVisit: trialVisit)
        addObservationFact('A:B', 102, valueType: 'N', numberValue: 99, textValue: 'E', trialVisit: trialVisit)
        addObservationFact('A:B', 103, valueType: 'N', numberValue: 40, textValue: 'GE', trialVisit: trialVisit)
        addObservationFact('A:B', 104, valueFlag: 'L', trialVisit: trialVisit)
        addObservationFact('A:B', 105, valueType: 'N', textValue: 'BAR', trialVisit: trialVisit)
        addObservationFact('A:C', 105, valueType: 'N', numberValue: 40, textValue: 'L', trialVisit: trialVisit)
        addObservationFact('A:C', 106, valueType: 'N', textValue: 'XPTO', trialVisit: trialVisit)

        /* 4. Flush session so these objects are available from SQL */
        sessionFactory.currentSession.flush()
    }

    void testBasic() {
        setupData()
        def definition = new QueryDefinition([
                new Panel(
                        invert: false,
                        items: [
                                new Item(
                                        conceptKey: '\\\\i2b2tc\\a\\'
                                )
                        ]
                )
        ])

        def result = queriesResourceService.runQuery(definition)
        expect:
        result allOf(
                hasProperty("id", notNullValue()),
                hasProperty("setSize", equalTo(7L /* 100-106 */)),
                hasProperty("status", equalTo(QueryStatus.FINISHED)),
                hasProperty("errorMessage", nullValue()),
        )
    }

    void testPanelInversion() {
        setupData()
        def definition = new QueryDefinition([
                new Panel(
                        items: [
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

        when:
        def result = queriesResourceService.runQuery(definition)
        then:
        result.setSize == 1L
        result.patients.size() == 1
        result.patients[0].id == 106L
    }

    void testRegularNumberValueConstraint() {
        setupData()
        def definition = new QueryDefinition([
                new Panel(
                        items: [
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

        when:
        def result = queriesResourceService.runQuery(definition)
        then:
        result.setSize == 2L
        result.patients.size() == 2
        102L in result.patients*.id
        105L in result.patients*.id
    }

    void testMultiplePanelsIntersectAtPatientLevel() {
        setupData()
        def definition = new QueryDefinition([
                new Panel(
                        items: [
                                new Item(
                                        conceptKey: '\\\\i2b2tc\\a\\b\\',
                                ),
                        ]
                ),
                new Panel(
                        items: [
                                new Item(
                                        conceptKey: '\\\\i2b2tc\\a\\c\\',
                                ),
                        ]
                )
        ])

        when:
        def result = queriesResourceService.runQuery(definition)
        then:
        result.setSize == 2L
        result.patients.size() == 2
        100L in result.patients*.id
        105L in result.patients*.id
    }

    void testBetweenValueConstraint() {
        setupData()
        def definition = new QueryDefinition([
                new Panel(
                        items: [
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

        when:
        def result = queriesResourceService.runQuery(definition)
        then:
        result.setSize == 2L
        result.patients.size() == 2
        100L in result.patients*.id
        101L in result.patients*.id
    }

    void testFlagConstraint() {
        setupData()
        def definition = new QueryDefinition([
                new Panel(
                        items: [
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
        when:
        def result = queriesResourceService.runQuery(definition)
        then:
        result.setSize == 1L
        result.patients.size() == 1
        result.patients[0].id == 104L
    }

    void testUnknownItemKeyResultsInInvalidRequestException() {
        setupData()
        def definition = new QueryDefinition([
                new Panel(
                        items: [
                                new Item(
                                        conceptKey: '\\\\bad table code\\a\\',
                                ),
                        ]
                )
        ])

        when:
        queriesResourceService.runQuery(definition)

        then:
        thrown(InvalidRequestException)
    }

    void testRunQueryAndFetchDefinitionAfterwards() {
        setupData()
        def inputDefinition = new QueryDefinition([
                new Panel(
                        items: [
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
        when:
        def result = queriesResourceService.runQuery(inputDefinition)
        then:
        result hasProperty('id', is(notNullValue()))

        when:
        def outputDefinition = queriesResourceService
                .getQueryDefinitionForResult(result)

        then:
        outputDefinition is(equalTo(inputDefinition))
    }

    void testFailingQuery() {
        setupData()
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

        when:
        QueryResult result = queriesResourceService.runQuery(inputDefinition)
        then:
        result.status == QueryStatus.ERROR
        cleanup:
        queriesResourceService.patientSetQueryBuilderService = orig
    }


    void testFailingQueryBuilding() {
        setupData()
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

        when:
        QueryResult result = queriesResourceService.runQuery(inputDefinition)
        then:
        result.status == QueryStatus.ERROR
        cleanup:
        queriesResourceService.patientSetQueryBuilderService = orig
    }

    void testGetQueryResultFromIdBasic() {
        setupData()
        def queryMaster = QueryResultData.createQueryResult([
                PatientDimension.load(100) // see setUp()
        ])
        queryMaster.save(failOnError: true)
        QueryResult savedResultInstance =
                QueryResultData.getQueryResultFromMaster(queryMaster)

        def result = queriesResourceService.getQueryResultFromId(
                savedResultInstance.id)

        expect:
        result allOf(
                hasProperty('id', is(savedResultInstance.id)),
                hasProperty('setSize', is(1L) /* only patient #100 */),
        )
    }

    void testQueryResultResultNonExistent() {
        setupData()

        when:
        queriesResourceService.getQueryResultFromId(-99112 /* bogus id */)

        then:
        thrown(NoSuchResourceException)
    }

    void testOverloadWithUsername() {
        setupData()
        def username = 'bogus_username'

        def definition = new QueryDefinition([
                new Panel(items: [
                        new Item(conceptKey: '\\\\i2b2tc\\a\\'),])])

        def result = queriesResourceService.runQuery(definition, username)

        expect:
        result hasProperty('username', is(username))
    }

}
