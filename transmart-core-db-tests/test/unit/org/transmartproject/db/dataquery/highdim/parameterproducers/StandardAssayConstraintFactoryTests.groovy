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

package org.transmartproject.db.dataquery.highdim.parameterproducers

import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.gmock.WithGMock
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.concept.ConceptKey
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.core.querytool.QueriesResource
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.dataquery.highdim.assayconstraints.*
import org.transmartproject.db.ontology.I2b2
import org.transmartproject.db.querytool.QtQueryResultInstance

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestMixin(GrailsUnitTestMixin)
@Mock([ I2b2, QtQueryResultInstance ])
@WithGMock
class StandardAssayConstraintFactoryTests {

    private StandardAssayConstraintFactory testee

    @Before
    void setUp() {
        testee = new StandardAssayConstraintFactory()
        testee.conceptsResource = mock(ConceptsResource)
        testee.queriesResource = mock(QueriesResource)
    }

    @Test
    void testCreateOntologyTermConstraint() {
        String conceptKey = '\\\\foo\\bar\\'

        testee.conceptsResource.getByKey(conceptKey).returns({
            // for some reason new I2b2(fullName: ...) does not work
            def r = new I2b2()
            r.fullName = new ConceptKey(conceptKey).conceptFullName
            r
        }())

        play {
            def result = testee.createOntologyTermConstraint concept_key: conceptKey

            assertThat result, allOf(
                    isA(DefaultOntologyTermCriteriaConstraint),
                    hasProperty('term', allOf(
                            isA(I2b2),
                            hasProperty('fullName', equalTo('\\bar\\'))
                    ))
            )
        }
    }

    @Test
    void testCreateOntologyTermBadArguments() {
        String conceptKey = '\\\\a\\b\\'

        testee.conceptsResource.getByKey(conceptKey).raises(NoSuchResourceException)

        play {
            assertThat shouldFail(InvalidArgumentsException) {
                //few arguments
                testee.createOntologyTermConstraint [:]
            }, containsString('exactly one parameter')

            assertThat shouldFail(InvalidArgumentsException) {
                //too many arguments
                testee.createOntologyTermConstraint([
                        concept_key: conceptKey,
                        another: 1,
                ])
            }, containsString('exactly one parameter')

            shouldFail(InvalidArgumentsException) {
                //ConceptsResource is raising NoSuchResourceException
                testee.createOntologyTermConstraint concept_key: conceptKey
            }
        }
    }

    @Test
    void testCreatePatientSetConstraint() {
        Long queryResultId = -11L
        QueryResult queryResult = new QtQueryResultInstance()

        testee.queriesResource.
                getQueryResultFromId(-11L).returns(queryResult)

        play {
            def result = testee.createPatientSetConstraint([
                    'result_instance_id': queryResultId
            ])

            assertThat result, allOf(
                    isA(DefaultPatientSetCriteriaConstraint),
                    hasProperty('queryResult', is(sameInstance(queryResult)))
            )
        }
    }

    @Test
    void testCreatePatientSetConstraintStringVariant() {
        String queryResultId = '-000011'
        QueryResult queryResult = new QtQueryResultInstance()

        testee.queriesResource.
                getQueryResultFromId(-11L).returns(queryResult)

        play {
            def result = testee.createPatientSetConstraint(
                    result_instance_id: queryResultId)

                    assertThat result, allOf(
                    isA(DefaultPatientSetCriteriaConstraint),
                    hasProperty('queryResult', is(sameInstance(queryResult)))
            )
        }
    }

    @Test
    void testCreatePatientSetConstraintBadArguments() {
        Long queryResultId = -12L

        testee.queriesResource.getQueryResultFromId(queryResultId).
                raises(NoSuchResourceException)

        play {
            assertThat shouldFail(InvalidArgumentsException) {
                //bad argument name
                testee.createPatientSetConstraint(foobar: -14L)
            }, containsString('is not in map')

            assertThat shouldFail(InvalidArgumentsException) {
                //bad argument value
                testee.createPatientSetConstraint(result_instance_id: 'mooor')
            }, containsString('Invalid value for')

            shouldFail(InvalidArgumentsException) {
                //ConceptsResource is raising NoSuchResourceException
                testee.createPatientSetConstraint result_instance_id: queryResultId
            }
        }
    }

    @Test
    void testCreateTrialNameConstraint() {
        def trialName = 'foobar'
        def result = testee.createTrialNameConstraint name: trialName

        assertThat result, allOf(
                isA(DefaultTrialNameCriteriaConstraint),
                hasProperty('trialName', equalTo(trialName))
        )
    }

    @Test
    void testCreateTrialNameConstraintBadArgument() {

        play {
            assertThat shouldFail(InvalidArgumentsException) {
                //few arguments
                testee.createTrialNameConstraint [:]
            }, containsString('Missing required parameter "name"')

            assertThat shouldFail(InvalidArgumentsException) {
                //bad argument name
                testee.createTrialNameConstraint bad_name: 'foobar'
            }, containsString('got the following parameters instead: [bad_name]')

            assertThat shouldFail(InvalidArgumentsException) {
                //bad type
                testee.createTrialNameConstraint name: [1]
            }, containsString('to be of type')

        }
    }

    @Test
    void testCreateAssayIdListConstraint() {
        AssayConstraint constraint = testee.createAssayIdListConstraint(ids: [0, '001'])
        assertThat constraint, allOf(
                isA(AssayIdListCriteriaConstraint),
                hasProperty('ids', contains(
                        is(0L), is(1L)))
        )
    }

    @Test
    void testCreateAssayIsListConstraintEmptyList() {
        assertThat shouldFail(InvalidArgumentsException) {
            testee.createAssayIdListConstraint(ids: [])
        }, containsString('empty list')
    }

    @Test
    void testCreateDisjunctionConstraintTwoDifferentTypes() {
        def trialName = 'foobar'

        AssayConstraint constraint = testee.createDisjunctionConstraint(
                { values, key -> testee.createFromParameters(key, values, null) },
                subconstraints: [
                        (AssayConstraint.TRIAL_NAME_CONSTRAINT): [
                                name: trialName
                        ],
                        (AssayConstraint.ASSAY_ID_LIST_CONSTRAINT): [
                                ids: [0]
                        ]
                ])

        assertThat constraint, allOf(
                isA(DisjunctionAssayCriteriaConstraint),
                hasProperty('constraints', containsInAnyOrder(
                        allOf(
                                isA(DefaultTrialNameCriteriaConstraint),
                                hasProperty('trialName', is(trialName))
                        ),
                        allOf(
                                isA(AssayIdListCriteriaConstraint),
                                hasProperty('ids', contains(0L))
                        ))))
    }

    @Test
    void testCreateDisjunctionConstraintTwoConstraintsOfSameType() {
        def trialNames = ['t1', 't2']

        AssayConstraint constraint = testee.createDisjunctionConstraint(
                { map, key -> testee.createFromParameters(key, map, null) },
                subconstraints: [
                        (AssayConstraint.TRIAL_NAME_CONSTRAINT): [
                                [name: trialNames[0]],
                                [name: trialNames[1]]]])

        assertThat constraint, allOf(
                isA(DisjunctionAssayCriteriaConstraint),
                hasProperty('constraints', containsInAnyOrder(
                        trialNames.collect {
                            allOf(
                                    isA(DefaultTrialNameCriteriaConstraint),
                                    hasProperty('trialName', is(it))
                            )
                        })))
    }

    @Test
    void testCreateNestedDisjunctionConstraint() {
        def trialNames = ['t1', 't2', 't3', 't4']
        def createFromParameters
        createFromParameters = { map, key -> testee.createFromParameters(key, map, createFromParameters) }

        AssayConstraint constraint = testee.createDisjunctionConstraint(
                createFromParameters,
                subconstraints: [
                        (AssayConstraint.DISJUNCTION_CONSTRAINT): [
                                [
                                    subconstraints: [
                                            (AssayConstraint.TRIAL_NAME_CONSTRAINT):  [
                                                    [name: trialNames[0]],
                                                    [name: trialNames[1]]]]
                                ],
                                [
                                    subconstraints: [
                                            (AssayConstraint.TRIAL_NAME_CONSTRAINT):  [
                                                    [name: trialNames[2]],
                                                    [name: trialNames[3]]]]]]])


        assertThat constraint, allOf(
                isA(DisjunctionAssayCriteriaConstraint),
                hasProperty('constraints', allOf(
                        everyItem(isA(DisjunctionAssayCriteriaConstraint)),
                        hasSize(2),
                        everyItem(
                                hasProperty('constraints', allOf(
                                        everyItem(isA(DefaultTrialNameCriteriaConstraint)),
                                        hasSize(2)))))))
    }

    @Test
    void testCreateDisjunctionOneSubconstraint() {
        def trialName = 'foobar'

        AssayConstraint constraint = testee.createDisjunctionConstraint(
                { map, key -> testee.createFromParameters(key, map, null) },
                subconstraints: [
                        (AssayConstraint.TRIAL_NAME_CONSTRAINT):  [
                                name: trialName]])

        assertThat constraint, allOf(
                isA(DefaultTrialNameCriteriaConstraint),
                hasProperty('trialName', equalTo(trialName)))
    }
}
