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

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.gmock.WithGMock
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.core.ontology.OntologyTerm
import spock.lang.Specification

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.Matchers.*
import static org.thehyve.commons.test.FastMatchers.propsWith
import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.CONTAINER

@WithGMock
@Integration
@Rollback

class AcrossTrialsConceptsResourceSpec extends Specification {

    public static final String AGE_AT_DIAGNOSIS_KEY =
            '\\\\xtrials\\Across Trials\\Demographics\\Age at Diagnosis\\'

    ConceptsResource testee
    ConceptsResource innerMock

    AcrossTrialsTestData testData

    def sessionFactory

    void setupData() {
        innerMock = mock(ConceptsResource)
        testee = new AcrossTrialsConceptsResourceDecorator(inner: innerMock)

        testData = AcrossTrialsTestData.createDefault()
        testData.saveAll()

        sessionFactory.currentSession.flush()
    }

    void testTopTermIsReturned() {
        setupData()
        innerMock.allCategories.returns([])

        play {
            def result = testee.allCategories
            expect:
            result contains(
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

    void testGetByKey() {
        setupData()
        OntologyTerm ageAtDiagnosis = testee.getByKey(AGE_AT_DIAGNOSIS_KEY)

        expect:
        ageAtDiagnosis
        hasProperty('key', equalTo(AGE_AT_DIAGNOSIS_KEY))
    }

    void testGetByKeyInexistentValidAcrossTrialsNode() {
        setupData()
        shouldFail NoSuchResourceException, {
            testee.getByKey('\\\\xtrials\\Across Trials\\i do not exist\\')
        }
    }

    void testGetByKeyInvalidAcrossTrialsNode() {
        setupData()
        // the first element is not "Across Trials"
        shouldFail NoSuchResourceException, {
            testee.getByKey('\\\\xtrials\\FOO BAR\\a\\')
        }
    }

    void testGetByKeyDelegatesToInner() {
        setupData()
        def key = '\\\\foobar\\Foo bar\\'
        def term = mock(OntologyTerm)
        innerMock.getByKey(key).returns(term)

        play {
            expect:
            testee.getByKey(key) is(sameInstance(term))
        }
    }
}
