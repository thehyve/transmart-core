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

import grails.test.mixin.TestFor
import org.transmartproject.core.concept.ConceptKey
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.ontology.OntologyTermsResource
import org.transmartproject.core.querytool.ConstraintByValue
import org.transmartproject.core.querytool.Item
import org.transmartproject.core.querytool.Panel
import org.transmartproject.core.querytool.QueryDefinition
import org.transmartproject.db.ontology.I2b2
import org.transmartproject.db.support.DatabasePortabilityService
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static org.transmartproject.core.querytool.ConstraintByValue.Operator.*
import static org.transmartproject.core.querytool.ConstraintByValue.ValueType.FLAG
import static org.transmartproject.core.querytool.ConstraintByValue.ValueType.NUMBER
import static org.transmartproject.db.support.DatabasePortabilityService.DatabaseType.POSTGRESQL

@TestFor(PatientSetQueryBuilderService)
class PatientSetQueryBuilderServiceSpec extends Specification {

    QtQueryResultInstance resultInstance

    void setup() {
        // doWithDynamicMethods is not run for unit tests...
        // Maybe making this an integration test would be preferable
        String.metaClass.asLikeLiteral = { replaceAll(/[\\%_]/, '\\\\$0') }

        def databasePortabilityStub = [
                getDatabaseType: { -> POSTGRESQL }
        ] as DatabasePortabilityService
        service.databasePortabilityService = databasePortabilityStub

        def ontologyTermsResourceServiceStub = [
                getByKey: { String key ->
                    def res = new I2b2(
                            factTableColumn: 'concept_cd',
                            dimensionTableName: 'concept_dimension',
                            columnName: 'concept_path',
                            columnDataType: 'T',
                            operator: 'LIKE',
                            dimensionCode: new ConceptKey(key).conceptFullName.toString(),
                    )
                    res.databasePortabilityService = databasePortabilityStub
                    res
                }
        ] as OntologyTermsResource
        service.ontologyTermsResourceService = ontologyTermsResourceServiceStub

        resultInstance = new QtQueryResultInstance()
        resultInstance.id = 42
    }

    void basicTest() {
        def conceptKey = '\\\\code\\full\\name\\'
        def definition = new QueryDefinition([
                new Panel(
                        invert: false,
                        items: [
                                new Item(
                                        conceptKey: conceptKey
                                )
                        ]
                )
        ])
        def sql = service.buildPatientSetQuery(resultInstance, definition)

        expect:
        sql allOf(
                startsWith('INSERT INTO qt_patient_set_collection'),
                containsString('SELECT patient_num FROM ' +
                        'observation_fact'),
                containsString('concept_cd IN (SELECT concept_cd FROM ' +
                        'concept_dimension WHERE concept_path LIKE ' +
                        '\'\\\\full\\\\name\\\\%\')')
        );
    }

    void testMultiplePanelsAndItems() {
        def definition = new QueryDefinition([
                new Panel(
                        invert: false,
                        items: [
                                new Item(
                                        conceptKey: '\\\\code\\full\\name1\\'
                                ),
                                new Item(
                                        conceptKey: '\\\\code\\full\\name2\\'
                                ),
                        ]
                ),
                new Panel(
                        items: [
                                new Item(
                                        conceptKey: '\\\\code\\full\\name3\\'
                                )
                        ]
                )
        ])
        def sql = service.buildPatientSetQuery(resultInstance, definition)

        expect:
        sql allOf(
                startsWith('INSERT INTO qt_patient_set_collection'),
                containsString('OR (concept_cd IN (SELECT concept_cd FROM ' +
                        'concept_dimension WHERE concept_path LIKE ' +
                        '\'\\\\full\\\\name2\\\\%\')'),
                containsString('INTERSECT (SELECT patient_num FROM ' +
                        'observation_fact WHERE (concept_cd IN (SELECT ' +
                        'concept_cd FROM concept_dimension WHERE concept_path ' +
                        'LIKE \'\\\\full\\\\name3\\\\%\''),
        );
    }

    void testPanelInversion() {
        def definition = new QueryDefinition([
                new Panel(
                        invert: true,
                        items: [
                                new Item(
                                        conceptKey: '\\\\code\\full\\name\\'
                                )
                        ]
                )
        ])
        def sql = service.buildPatientSetQuery(resultInstance, definition)

        expect:
        sql.contains('SELECT patient_num FROM ' +
                'patient_dimension EXCEPT (SELECT patient_num FROM ' +
                'observation_fact WHERE (concept_cd IN (SELECT concept_cd ' +
                'FROM concept_dimension WHERE concept_path ' +
                'LIKE \'\\\\full\\\\name\\\\%\')) ' +
                'AND concept_cd != \'SECURITY\') ORDER BY 1')
    }

    void testPanelInversionPlacement() {
        def definition = new QueryDefinition([
                new Panel(
                        invert: true,
                        items: [
                                new Item(
                                        conceptKey: '\\\\code\\b\\'
                                )
                        ]
                ),
                new Panel(
                        items: [
                                new Item(
                                        conceptKey: '\\\\code\\a\\'
                                )
                        ]
                )
        ])

        def sql = service.buildPatientSetQuery(resultInstance, definition)
        expect:
        sql.contains('EXCEPT (SELECT patient_num ' +
                'FROM observation_fact WHERE (concept_cd IN (SELECT ' +
                'concept_cd FROM concept_dimension WHERE concept_path ' +
                'LIKE \'\\\\b\\\\%\')) AND concept_cd != \'SECURITY\')')
    }

    void testNumberSimpleConstraint() {
        def definition = new QueryDefinition([
                new Panel(
                        items: [
                                new Item(
                                        conceptKey: '\\\\code\\full\\name\\',
                                        constraint: new ConstraintByValue(
                                                operator: LOWER_THAN,
                                                valueType: NUMBER,
                                                constraint: '5.6'
                                        )
                                )
                        ]
                )
        ])

        def sql = service.buildPatientSetQuery(resultInstance, definition)

        expect:
        sql.contains("AND ((valtype_cd = 'N' " +
                "AND nval_num < 5.6 AND tval_char IN ('E', 'LE')) OR (" +
                "valtype_cd = 'N' AND nval_num <= 5.6 AND tval_char = 'L'))")
    }

    void testNumberBetweenConstraint() {
        def definition = new QueryDefinition([
                new Panel(
                        items: [
                                new Item(
                                        conceptKey: '\\\\code\\full\\name\\',
                                        constraint: new ConstraintByValue(
                                                operator: BETWEEN,
                                                valueType: NUMBER,
                                                constraint: '5.6 and 5.8'
                                        )
                                )
                        ]
                )
        ])

        def sql = service.buildPatientSetQuery(resultInstance, definition)

        expect:
        sql.contains("AND ((valtype_cd = 'N' AND " +
                "nval_num BETWEEN 5.6 AND 5.8 AND tval_char = 'E')))")
    }

    void testFlagConstraint() {
        def definition = new QueryDefinition([
                new Panel(
                        items: [
                                new Item(
                                        conceptKey: '\\\\code\\full\\name\\',
                                        constraint: new ConstraintByValue(
                                                operator: EQUAL_TO,
                                                valueType: FLAG,
                                                constraint: 'N'
                                        )
                                )
                        ]
                )
        ])

        def sql = service.buildPatientSetQuery(resultInstance, definition)

        expect:
        sql.contains("AND (valueflag_cd = 'N')")
    }

    // The rest are error tests

    void testNoPanel() {
        def definition = new QueryDefinition([])

        when:
        service.buildPatientSetQuery(resultInstance, definition)
        then:
        def e = thrown(InvalidRequestException)
        e.message == 'No panels were specified'
    }

    void testEmptyPanel() {
        def definition = new QueryDefinition([
                new Panel(items: [])
        ])

        when:
        service.buildPatientSetQuery(resultInstance, definition)
        then:
        def e = thrown(InvalidRequestException)
        e.message == 'Found panel with no items'
    }

    void testNullItem() {
        def definition = new QueryDefinition([
                new Panel(items: [null])
        ])
        when:
        service.buildPatientSetQuery(resultInstance, definition)
        then:
        def e = thrown(InvalidRequestException)
        e.message == 'Found panel with null value in its item list'
    }

    void testNullConceptKey() {
        def definition = new QueryDefinition([
                new Panel(items: [
                        new Item(conceptKey: null)
                ])
        ])
        when:
        service.buildPatientSetQuery(resultInstance, definition)
        then:
        def e = thrown(InvalidRequestException)
        e.message == 'Found item with null conceptKey'
    }

    void testConstraintWithoutOperator() {
        def definition = new QueryDefinition([
                new Panel(items: [
                        new Item(
                                conceptKey: '\\\\code\\full\\name\\',
                                constraint: new ConstraintByValue(
                                        valueType: FLAG,
                                        constraint: 'N'
                                )
                        )
                ])
        ])
        when:
        service.buildPatientSetQuery(resultInstance, definition)
        then:
        def e = thrown(InvalidRequestException)
        e.message == 'Found item constraint with null operator'
    }

    void testConstraintWithoutValueType() {
        def definition = new QueryDefinition([
                new Panel(items: [
                        new Item(
                                conceptKey: '\\\\code\\full\\name\\',
                                constraint: new ConstraintByValue(
                                        operator: EQUAL_TO,
                                        constraint: 'N'
                                )
                        )
                ])
        ])
        when:
        service.buildPatientSetQuery(resultInstance, definition)
        then:
        def e = thrown(InvalidRequestException)
        e.message == 'Found item constraint with null value type'
    }

    void testConstraintWithoutConstraintValue() {
        def definition = new QueryDefinition([
                new Panel(items: [
                        new Item(
                                conceptKey: '\\\\code\\full\\name\\',
                                constraint: new ConstraintByValue(
                                        operator: EQUAL_TO,
                                        valueType: FLAG,
                                )
                        )
                ])
        ])
        when:
        service.buildPatientSetQuery(resultInstance, definition)
        then:
        def e = thrown(InvalidRequestException)
        e.message == 'Found item constraint with null constraint value'
    }

    void testBogusConstraintFlagValue() {
        def definition = new QueryDefinition([
                new Panel(items: [
                        new Item(
                                conceptKey: '\\\\code\\full\\name\\',
                                constraint: new ConstraintByValue(
                                        operator: EQUAL_TO,
                                        valueType: FLAG,
                                        constraint: 'FOO'
                                )
                        )
                ])
        ])
        when:
        service.buildPatientSetQuery(resultInstance, definition)
        then:
        def e = thrown(InvalidRequestException)
        e.message.contains("A flag value constraint's operand must be either 'L', 'H' or 'N'")
    }

    void testBogusConstraintNumberValue() {
        def definition = new QueryDefinition([
                new Panel(items: [
                        new Item(
                                conceptKey: '\\\\code\\full\\name\\',
                                constraint: new ConstraintByValue(
                                        operator: EQUAL_TO,
                                        valueType: NUMBER,
                                        constraint: 'FOO'
                                )
                        )
                ])
        ])
        when:
        service.buildPatientSetQuery(resultInstance, definition)
        then:
        def e = thrown(InvalidRequestException)
        e.message.contains('an invalid number constraint value')
    }

    void testBogusBetweenConstraintNumberValue() {
        def definition = new QueryDefinition([
                new Panel(items: [
                        new Item(
                                conceptKey: '\\\\code\\full\\name\\',
                                constraint: new ConstraintByValue(
                                        operator: BETWEEN,
                                        valueType: NUMBER,
                                        constraint: '5.6'
                                )
                        )
                ])
        ])
        when:
        service.buildPatientSetQuery(resultInstance, definition)
        then:
        def e = thrown(InvalidRequestException)
        e.message.contains('an invalid number constraint value')
    }

    void testInvalidOperatorForFlagValueContraint() {
        def definition = new QueryDefinition([
                new Panel(items: [
                        new Item(
                                conceptKey: '\\\\code\\full\\name\\',
                                constraint: new ConstraintByValue(
                                        operator: LOWER_OR_EQUAL_TO,
                                        valueType: FLAG,
                                        constraint: '5.6'
                                )
                        )
                ])
        ])
        when:
        service.buildPatientSetQuery(resultInstance, definition)
        then:
        def e = thrown(InvalidRequestException)
        e.message.contains('Found item flag constraint with an operator different from EQUAL_TO')
    }

}
