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
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.OntologyTermsResource
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.db.TestData
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static org.thehyve.commons.test.FastMatchers.propsWith
import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.CONTAINER

@Integration
@Rollback
class AcrossTrialsConceptsResourceSpec extends Specification {

    public static final String AGE_AT_DIAGNOSIS_KEY =
            '\\\\xtrials\\Across Trials\\Demographics\\Age at Diagnosis\\'

    OntologyTermsResource testee
    OntologyTermsResource innerMock

    AcrossTrialsTestData testData

    def sessionFactory

    void setupData() {
        TestData.prepareCleanDatabase()

        innerMock = Mock(OntologyTermsResource)
        testee = new AcrossTrialsConceptsResourceDecorator(inner: innerMock)

        testData = AcrossTrialsTestData.createDefault()
        testData.saveAll()
    }

    void testTopTermIsReturned() {
        setupData()
        innerMock.allCategories >> []

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

    void testGetByKey() {
        setupData()
        OntologyTerm ageAtDiagnosis = testee.getByKey(AGE_AT_DIAGNOSIS_KEY)

        expect:
        ageAtDiagnosis
        hasProperty('key', equalTo(AGE_AT_DIAGNOSIS_KEY))
    }

    void testGetByKeyInexistentValidAcrossTrialsNode() {
        setupData()

        when:
        testee.getByKey('\\\\xtrials\\Across Trials\\i do not exist\\')
        then:
        thrown(NoSuchResourceException)
    }

    void testGetByKeyInvalidAcrossTrialsNode() {
        setupData()
        // the first element is not "Across Trials"
        when:
        testee.getByKey('\\\\xtrials\\FOO BAR\\a\\')
        then:
        thrown(NoSuchResourceException)
    }

    void testGetByKeyDelegatesToInner() {
        setupData()
        def key = '\\\\foobar\\Foo bar\\'
        def term = Mock(OntologyTerm)
        innerMock.getByKey(key) >> term

        def result = testee.getByKey(key)

        expect:
        result is(sameInstance(term))
    }
}
