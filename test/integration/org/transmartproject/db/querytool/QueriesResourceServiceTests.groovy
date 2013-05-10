package org.transmartproject.db.querytool

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.querytool.ConstraintByValue
import org.transmartproject.core.querytool.Item
import org.transmartproject.core.querytool.Panel
import org.transmartproject.core.querytool.QueryDefinition
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.querytool.QueryStatus
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.ontology.ConceptTestData

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import org.junit.*

import static org.transmartproject.core.querytool.ConstraintByValue.Operator.*
import static org.transmartproject.core.querytool.ConstraintByValue.ValueType.*

@Mixin(ConceptTestData)
class QueriesResourceServiceTests extends GroovyTestCase {

    def queriesResourceService
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
                conceptCd:  conceptCd,
                patientNum: patientNum,
        ])
        def ret = fact.save()
        assertThat(ret, is(notNullValue()))
    }

    private void addConceptDimension(String conceptCd, String conceptPath) {
        ConceptDimension conceptDimension = new ConceptDimension(
                conceptCd:   conceptCd,
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
        addObservationFact('A:B', 100, valtypeCd: 'N', nvalNum: 50, tvalChar: 'E')
        addObservationFact('A:C', 100, valtypeCd: 'T', tvalChar: 'FOO')
        addObservationFact('A:B', 101, valtypeCd: 'N', nvalNum: 75, tvalChar: 'E')
        addObservationFact('A:B', 102, valtypeCd: 'N', nvalNum: 99, tvalChar: 'E')
        addObservationFact('A:B', 103, valtypeCd: 'N', nvalNum: 40, tvalChar: 'GE')
        addObservationFact('A:B', 104, valueflagCd: 'L')
        addObservationFact('A:B', 105, valtypeCd: 'N', tvalChar: 'BAR')
        addObservationFact('A:C', 105, valtypeCd: 'N', nvalNum: 40, tvalChar: 'L')
        addObservationFact('A:C', 106, valtypeCd: 'N', tvalChar: 'XPTO')

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

}
