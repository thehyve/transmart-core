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

import org.transmartproject.core.concept.ConceptKey
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.OntologyTermsResource
import org.transmartproject.core.querytool.QueriesResource
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.dataquery.highdim.assayconstraints.*
import org.transmartproject.db.ontology.I2b2
import org.transmartproject.db.querytool.QtQueryResultInstance
import spock.lang.Specification

import static org.hamcrest.Matchers.*

class StandardAssayConstraintFactorySpec extends Specification {

    private StandardAssayConstraintFactory testee

    void setup() {
        testee = new StandardAssayConstraintFactory()
        testee.conceptsResource = Mock(OntologyTermsResource)
        testee.queriesResource = Mock(QueriesResource)
    }

    void testCreateOntologyTermConstraint() {
        String conceptKey = '\\\\foo\\bar\\'

        testee.conceptsResource.getByKey(conceptKey) >> {
            // for some reason new I2b2(fullName: ...) does not work
            def r = new I2b2()
            r.fullName = new ConceptKey(conceptKey).conceptFullName
            r
        }()

        def result = testee.createOntologyTermConstraint concept_key: conceptKey

        expect:
        result instanceof DefaultConceptPathCriteriaConstraint
        result.conceptPath == '\\bar\\'
    }

    void testCreateOntologyTermBadArguments() {
        String conceptKey = '\\\\a\\b\\'

        testee.conceptsResource.getByKey(_) >> { ck ->
            assert ck[0] == conceptKey
            throw new NoSuchResourceException()
        }

        when:
        testee.createOntologyTermConstraint [:]
        then:
        def e1 = thrown(InvalidArgumentsException)
        e1.message.contains('exactly one parameter')

        when:
        testee.createOntologyTermConstraint([
                concept_key: conceptKey,
                another    : 1,
        ])
        then:
        def e2 = thrown(InvalidArgumentsException)
        e2.message.contains('exactly one parameter')

        when:
        testee.createOntologyTermConstraint concept_key: conceptKey
        then:
        thrown(InvalidArgumentsException)
    }

    void testCreatePatientSetConstraint() {
        Long queryResultId = -11L
        QueryResult queryResult = new QtQueryResultInstance()

        testee.queriesResource.
                getQueryResultFromId(-11L) >> queryResult

        when:
        def result = testee.createPatientSetConstraint([
                'result_instance_id': queryResultId
        ])

        then:
        result instanceof DefaultPatientSetCriteriaConstraint
        result.queryResult.is(queryResult)
    }

    void testCreatePatientSetConstraintStringVariant() {
        String queryResultId = '-000011'
        QueryResult queryResult = new QtQueryResultInstance()

        testee.queriesResource.
                getQueryResultFromId(-11L) >> queryResult

        def result = testee.createPatientSetConstraint(
                result_instance_id: queryResultId)

        expect:
        result instanceof DefaultPatientSetCriteriaConstraint
        result.queryResult.is(queryResult)
    }

    void testCreatePatientSetConstraintBadArguments() {
        Long queryResultId = -12L

        testee.queriesResource.getQueryResultFromId(_) >> { qri ->
            assert qri[0] == queryResultId
            throw new NoSuchResourceException()
        }

        when: 'bad argument name'
        testee.createPatientSetConstraint(foobar: -14L)
        then:
        def e1 = thrown(InvalidArgumentsException)
        e1.message.contains('is not in map')

        when: 'bad argument value'
        testee.createPatientSetConstraint(result_instance_id: 'mooor')
        then:
        def e2 = thrown(InvalidArgumentsException)
        e2.message.contains('Invalid value for')

        when: 'ConceptsResource is raising NoSuchResourceException'
        testee.createPatientSetConstraint result_instance_id: queryResultId
        then:
        def e3 = thrown(InvalidArgumentsException)
        e3.cause instanceof NoSuchResourceException
    }

    void testCreateTrialNameConstraint() {
        def trialName = 'foobar'
        def result = testee.createTrialNameConstraint name: trialName

        expect:
        result allOf(
                isA(DefaultTrialNameCriteriaConstraint),
                hasProperty('trialName', equalTo(trialName))
        )
    }

    void testCreateTrialNameConstraintBadArgument() {
        when:
        testee.createTrialNameConstraint [:]
        then:
        def e1 = thrown(InvalidArgumentsException)
        e1.message.contains('Missing required parameter "name"')

        when:
        testee.createTrialNameConstraint bad_name: 'foobar'
        then:
        def e2 = thrown(InvalidArgumentsException)
        e2.message.contains('got the following parameters instead: [bad_name]')

        when:
        testee.createTrialNameConstraint name: [1]
        then:
        def e3 = thrown(InvalidArgumentsException)
        e3.message.contains('to be of type')
    }

    void testCreateAssayIdListConstraint() {
        AssayConstraint constraint = testee.createAssayIdListConstraint(ids: [0, '001'])
        expect:
        constraint allOf(
                isA(AssayIdListCriteriaConstraint),
                hasProperty('ids', contains(
                        is(0L), is(1L)))
        )
    }

    void testCreateAssayIsListConstraintEmptyList() {
        when:
        testee.createAssayIdListConstraint(ids: [])
        then:
        def e1 = thrown(InvalidArgumentsException)
        e1.message.contains('empty list')
    }

    void testCreatePatientIdListConstraint() {
        AssayConstraint constraint = testee.createPatientIdListConstraint(ids: [0, '001', "FXQ1"])
        expect:
        constraint allOf(
                isA(PatientIdListCriteriaConstraint),
                hasProperty('patientIdList', contains(
                        is("0"), is("001"), is("FXQ1")))
        )
    }

    void testCreatePatientIsListConstraintEmptyList() {
        when:
        testee.createPatientIdListConstraint(ids: [])
        then:
        def e = thrown(InvalidArgumentsException)
        e.message.contains('empty list')
    }

    void testCreateDisjunctionConstraintTwoDifferentTypes() {
        def trialName = 'foobar'

        AssayConstraint constraint = testee.createDisjunctionConstraint(
                { values, key -> testee.createFromParameters(key, values, null) },
                subconstraints: [
                        (AssayConstraint.TRIAL_NAME_CONSTRAINT)   : [
                                name: trialName
                        ],
                        (AssayConstraint.ASSAY_ID_LIST_CONSTRAINT): [
                                ids: [0]
                        ]
                ])

        expect:
        constraint allOf(
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

        expect:
        constraint allOf(
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
                                                (AssayConstraint.TRIAL_NAME_CONSTRAINT): [
                                                        [name: trialNames[0]],
                                                        [name: trialNames[1]]]]
                                ],
                                [
                                        subconstraints: [
                                                (AssayConstraint.TRIAL_NAME_CONSTRAINT): [
                                                        [name: trialNames[2]],
                                                        [name: trialNames[3]]]]]]])


        expect:
        constraint allOf(
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
                        (AssayConstraint.TRIAL_NAME_CONSTRAINT): [
                                name: trialName]])

        expect:
        constraint allOf(
                isA(DefaultTrialNameCriteriaConstraint),
                hasProperty('trialName', equalTo(trialName)))
    }
}
