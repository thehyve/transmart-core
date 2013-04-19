package org.transmartproject.db.querytool

import org.junit.rules.ExpectedException
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.querytool.ConstraintByValue
import org.transmartproject.core.querytool.Item
import org.transmartproject.core.querytool.Panel
import org.transmartproject.core.querytool.QueryDefinition
import org.transmartproject.db.concept.ConceptKey
import org.transmartproject.db.ontology.ConceptsResourceService
import org.transmartproject.db.ontology.I2b2
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.core.querytool.ConstraintByValue.Operator.*
import static org.transmartproject.core.querytool.ConstraintByValue.ValueType.*

@TestMixin(GrailsUnitTestMixin)
@TestFor(PatientSetQueryBuilderService)
class PatientSetQueryBuilderServiceTests {

    QtQueryResultInstance resultInstance

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    void setUp() {
        // doWithDynamicMethods is not run for unit tests...
        // Maybe making this an integration test would be preferable
        String.metaClass.asLikeLiteral = { replaceAll(/[\\%_]/, '\\\\$0') }

        def conceptsResourceServiceStub = [
                getByKey: { String key ->
                    new I2b2(
                            factTableColumn    : 'concept_cd',
                            dimensionTableName : 'concept_dimension',
                            columnName         : 'concept_path',
                            columnDataType     : 'T',
                            operator           : 'LIKE',
                            dimensionCode      : new ConceptKey(key).conceptFullName.toString(),
                    )
                }
        ] as ConceptsResourceService
        service.conceptsResourceService = conceptsResourceServiceStub

        resultInstance = new QtQueryResultInstance()
        resultInstance.id = 42
    }

    @Test
    void basicTest() {
        def conceptKey = '\\\\code\\full\\name\\'
        def definition = new QueryDefinition([
                new Panel(
                        invert: false,
                        items:  [
                                new Item(
                                        conceptKey: conceptKey
                                )
                        ]
                )
        ])
        def sql = service.buildPatientSetQuery(resultInstance, definition)

        assertThat sql, allOf(
                startsWith('INSERT INTO qt_patient_set_collection'),
                containsString('SELECT patient_num FROM ' +
                        'observation_fact'),
                containsString('concept_cd IN (SELECT concept_cd FROM ' +
                        'concept_dimension WHERE concept_path LIKE ' +
                        '\'\\\\full\\\\name\\\\%\')')
                );
    }

    @Test
    void testMultiplePanelsAndItems() {
        def definition = new QueryDefinition([
                new Panel(
                        invert: false,
                        items:  [
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

        assertThat sql, allOf(
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

    @Test
    void testPanelInversion() {
        def definition = new QueryDefinition([
                new Panel(
                        invert: true,
                        items:  [
                                new Item(
                                        conceptKey: '\\\\code\\full\\name\\'
                                )
                        ]
                )
        ])
        def sql = service.buildPatientSetQuery(resultInstance, definition)

        assertThat sql, containsString('SELECT patient_num FROM ' +
                'patient_dimension EXCEPT (SELECT patient_num FROM ' +
                'observation_fact WHERE (concept_cd IN (SELECT concept_cd ' +
                'FROM concept_dimension WHERE concept_path ' +
                'LIKE \'\\\\full\\\\name\\\\%\'))) ORDER BY 1')
    }

    @Test
    void testPanelInversionPlacement() {
        def definition = new QueryDefinition([
                new Panel(
                        invert: true,
                        items:  [
                                new Item(
                                        conceptKey: '\\\\code\\b\\'
                                )
                        ]
                ),
                new Panel(
                        items:  [
                                new Item(
                                        conceptKey: '\\\\code\\a\\'
                                )
                        ]
                )
        ])

        def sql = service.buildPatientSetQuery(resultInstance, definition)
        assertThat sql, containsString('EXCEPT (SELECT patient_num ' +
                'FROM observation_fact WHERE (concept_cd IN (SELECT ' +
                'concept_cd FROM concept_dimension WHERE concept_path ' +
                'LIKE \'\\\\b\\\\%\')))')
    }

    @Test
    void testNumberSimpleConstraint() {
        def definition = new QueryDefinition([
                new Panel(
                        items:  [
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

        assertThat sql, containsString("AND ((valtype_cd = 'N' " +
                "AND nval_num < 5.6 AND tval_char IN ('E', 'LE')) OR (" +
                "valtype_cd = 'N' AND nval_num <= 5.6 AND tval_char = 'L'))")
    }

    @Test
    void testNumberBetweenConstraint() {
        def definition = new QueryDefinition([
                new Panel(
                        items:  [
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

        assertThat sql, containsString("AND ((valtype_cd = 'N' AND " +
                "nval_num BETWEEN 5.6 AND 5.8 AND tval_char = 'E')))")
    }

    @Test
    void testFlagConstraint() {
        def definition = new QueryDefinition([
                new Panel(
                        items:  [
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

        assertThat sql, containsString("AND (valueflag_cd = 'N')")
    }


    // The rest are error tests

    @Test
    void testNoPanel() {
        expectedException.expect(isA(InvalidRequestException))
        expectedException.expectMessage(equalTo('No panels were specified'))

        def definition = new QueryDefinition([])

        service.buildPatientSetQuery(resultInstance, definition)
    }

    @Test
    void testEmptyPanel() {
        expectedException.expect(isA(InvalidRequestException))
        expectedException.expectMessage(equalTo('Found panel with no items'))

        def definition = new QueryDefinition([
                new Panel(items: [])
        ])

        service.buildPatientSetQuery(resultInstance, definition)
    }

    @Test
    void testNullItem() {
        expectedException.expect(isA(InvalidRequestException))
        expectedException.expectMessage(equalTo('Found panel with null value ' +
                'in its item list'))

        def definition = new QueryDefinition([
                new Panel(items: [null])
        ])

        service.buildPatientSetQuery(resultInstance, definition)
    }

    @Test
    void testNullConceptKey() {
        expectedException.expect(isA(InvalidRequestException))
        expectedException.expectMessage(
                equalTo('Found item with null conceptKey'))

        def definition = new QueryDefinition([
                new Panel(items: [
                        new Item(conceptKey: null)
                ])
        ])

        service.buildPatientSetQuery(resultInstance, definition)
    }

    @Test
    void testConstraintWithoutOperator() {
        expectedException.expect(isA(InvalidRequestException))
        expectedException.expectMessage(equalTo('Found item constraint with ' +
                'null operator'))

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

        service.buildPatientSetQuery(resultInstance, definition)
    }

    @Test
    void testConstraintWithoutValueType() {
        expectedException.expect(isA(InvalidRequestException))
        expectedException.expectMessage(equalTo('Found item constraint with ' +
                'null value type'))

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

        service.buildPatientSetQuery(resultInstance, definition)
    }

    @Test
    void testConstraintWithoutConstraintValue() {
        expectedException.expect(isA(InvalidRequestException))
        expectedException.expectMessage(equalTo('Found item constraint with ' +
                'null constraint value'))

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

        service.buildPatientSetQuery(resultInstance, definition)
    }

    @Test
    void testBogusConstraintFlagValue() {
        expectedException.expect(isA(InvalidRequestException))
        expectedException.expectMessage(containsString('A flag value ' +
                "constraint's operand must be either 'L', 'H' or 'N'"))

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

        service.buildPatientSetQuery(resultInstance, definition)
    }

    @Test
    void testBogusConstraintNumberValue() {
        expectedException.expect(isA(InvalidRequestException))
        expectedException.expectMessage(containsString('an invalid number ' +
                'constraint value'))

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

        service.buildPatientSetQuery(resultInstance, definition)
    }

    @Test
    void testBogusBetweenConstraintNumberValue() {
        expectedException.expect(isA(InvalidRequestException))
        expectedException.expectMessage(containsString('an invalid number ' +
                'constraint value'))

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

        service.buildPatientSetQuery(resultInstance, definition)
    }

    @Test
    void testInvalidOperatorForFlagValueContraint() {
        expectedException.expect(isA(InvalidRequestException))
        expectedException.expectMessage(equalTo('Found item flag constraint ' +
                'with an operator different from EQUAL_TO'))

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

        service.buildPatientSetQuery(resultInstance, definition)
    }

}
