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

import groovy.util.logging.Slf4j
import spock.lang.Specification

import grails.test.mixin.Mock
import org.gmock.WithGMock
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

import static org.hamcrest.Matchers.*

@Mock([ I2b2, QtQueryResultInstance ])
@WithGMock
@Slf4j
class StandardAssayConstraintFactorySpec extends Specification {

    private StandardAssayConstraintFactory testee

    void setup() {
        testee = new StandardAssayConstraintFactory()
        testee.conceptsResource = mock(ConceptsResource)
        testee.queriesResource = mock(QueriesResource)
    }

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

            expect: result allOf(
                    isA(DefaultOntologyTermCriteriaConstraint),
                    hasProperty('term', allOf(
                            isA(I2b2),
                            hasProperty('fullName', equalTo('\\bar\\'))
                    ))
            )
        }
    }

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

    void testCreatePatientSetConstraint() {
        Long queryResultId = -11L
        QueryResult queryResult = new QtQueryResultInstance()

        testee.queriesResource.
                getQueryResultFromId(-11L).returns(queryResult)

        play {
            def result = testee.createPatientSetConstraint([
                    'result_instance_id': queryResultId
            ])

            expect: result allOf(
                    isA(DefaultPatientSetCriteriaConstraint),
                    hasProperty('queryResult', is(sameInstance(queryResult)))
            )
        }
    }

    void testCreatePatientSetConstraintStringVariant() {
        String queryResultId = '-000011'
        QueryResult queryResult = new QtQueryResultInstance()

        testee.queriesResource.
                getQueryResultFromId(-11L).returns(queryResult)

        play {
            def result = testee.createPatientSetConstraint(
                    result_instance_id: queryResultId)

                    expect: result allOf(
                    isA(DefaultPatientSetCriteriaConstraint),
                    hasProperty('queryResult', is(sameInstance(queryResult)))
            )
        }
    }

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

    void testCreateTrialNameConstraint() {
        def trialName = 'foobar'
        def result = testee.createTrialNameConstraint name: trialName

        expect: result allOf(
                isA(DefaultTrialNameCriteriaConstraint),
                hasProperty('trialName', equalTo(trialName))
        )
    }

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

    void testCreateAssayIdListConstraint() {
        AssayConstraint constraint = testee.createAssayIdListConstraint(ids: [0, '001'])
        expect: constraint allOf(
                isA(AssayIdListCriteriaConstraint),
                hasProperty('ids', contains(
                        is(0L), is(1L)))
        )
    }

    void testCreateAssayIsListConstraintEmptyList() {
        assertThat shouldFail(InvalidArgumentsException) {
            testee.createAssayIdListConstraint(ids: [])
        }, containsString('empty list')
    }

    void testCreatePatientIdListConstraint() {
        AssayConstraint constraint = testee.createPatientIdListConstraint(ids: [0, '001', "FXQ1"])
        expect: constraint allOf(
                isA(PatientIdListCriteriaConstraint),
                hasProperty('patientIdList', contains(
                        is("0"), is("001"), is("FXQ1")))
        )
    }

    void testCreatePatientIsListConstraintEmptyList() {
        assertThat shouldFail(InvalidArgumentsException) {
            testee.createPatientIdListConstraint(ids: [])
        }, containsString('empty list')
    }

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

        expect: constraint allOf(
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

    void testCreateDisjunctionConstraintTwoConstraintsOfSameType() {
        def trialNames = ['t1', 't2']

        AssayConstraint constraint = testee.createDisjunctionConstraint(
                { map, key -> testee.createFromParameters(key, map, null) },
                subconstraints: [
                        (AssayConstraint.TRIAL_NAME_CONSTRAINT): [
                                [name: trialNames[0]],
                                [name: trialNames[1]]]])

        expect: constraint allOf(
                isA(DisjunctionAssayCriteriaConstraint),
                hasProperty('constraints', containsInAnyOrder(
                        trialNames.collect {
                            allOf(
                                    isA(DefaultTrialNameCriteriaConstraint),
                                    hasProperty('trialName', is(it))
                            )
                        })))
    }

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


        expect: constraint allOf(
                isA(DisjunctionAssayCriteriaConstraint),
                hasProperty('constraints', allOf(
                        everyItem(isA(DisjunctionAssayCriteriaConstraint)),
                        hasSize(2),
                        everyItem(
                                hasProperty('constraints', allOf(
                                        everyItem(isA(DefaultTrialNameCriteriaConstraint)),
                                        hasSize(2)))))))
    }

    void testCreateDisjunctionOneSubconstraint() {
        def trialName = 'foobar'

        AssayConstraint constraint = testee.createDisjunctionConstraint(
                { map, key -> testee.createFromParameters(key, map, null) },
                subconstraints: [
                        (AssayConstraint.TRIAL_NAME_CONSTRAINT):  [
                                name: trialName]])

        expect: constraint allOf(
                isA(DefaultTrialNameCriteriaConstraint),
                hasProperty('trialName', equalTo(trialName)))
    }
}
