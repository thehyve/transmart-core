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

package org.transmartproject.db.ontology

import grails.test.mixin.TestMixin
import org.gmock.WithGMock
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.thehyve.commons.test.FastMatchers.propsWith
import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.CONTAINER

@TestMixin(RuleBasedIntegrationTestMixin)
@WithGMock
class AcrossTrialsConceptsResourceTests {

    public static final String AGE_AT_DIAGNOSIS_KEY =
            '\\\\xtrials\\Across Trials\\Demographics\\Age at Diagnosis\\'

    ConceptsResource testee
    ConceptsResource innerMock

    AcrossTrialsTestData testData

    def sessionFactory

    @Before
    void setUp() {
        innerMock = mock(ConceptsResource)
        testee = new AcrossTrialsConceptsResourceDecorator(inner: innerMock)

        testData = AcrossTrialsTestData.createDefault()
        testData.saveAll()

        sessionFactory.currentSession.flush()
    }

    @Test
    void testTopTermIsReturned() {
        innerMock.allCategories.returns([])

        play {
            def result = testee.allCategories
            assertThat result, contains(
                    propsWith(
                            level: 0,
                            key: '\\\\xtrials\\Across Trials\\',
                            fullName: '\\Across Trials\\',
                            study: is(nullValue()),
                            name: 'Across Trials',
                            tooltip: 'Across Trials',
                            visualAttributes: contains(CONTAINER),
                            metadata: is(nullValue()),
                    )
            )
        }
    }

    @Test
    void testGetByKey() {
        OntologyTerm ageAtDiagnosis = testee.getByKey(AGE_AT_DIAGNOSIS_KEY)

        assertThat ageAtDiagnosis,
                hasProperty('key', equalTo(AGE_AT_DIAGNOSIS_KEY))
    }

    @Test
    void testGetByKeyInexistentValidAcrossTrialsNode() {
        shouldFail NoSuchResourceException, {
            testee.getByKey('\\\\xtrials\\Across Trials\\i do not exist\\')
        }
    }

    @Test
    void testGetByKeyInvalidAcrossTrialsNode() {
        // the first element is not "Across Trials"
        shouldFail NoSuchResourceException, {
            testee.getByKey('\\\\xtrials\\FOO BAR\\a\\')
        }
    }

    @Test
    void testGetByKeyDelegatesToInner() {
        def key = '\\\\foobar\\Foo bar\\'
        def term = mock(OntologyTerm)
        innerMock.getByKey(key).returns(term)

        play {
            assertThat testee.getByKey(key), is(sameInstance(term))
        }
    }
}
