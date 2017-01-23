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
import org.junit.Before
import org.junit.Test
import org.thehyve.commons.test.FastMatchers
import org.transmartproject.core.concept.ConceptKey
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.LEAF

@TestMixin(RuleBasedIntegrationTestMixin)
class AcrossTrialsOntologyTermTests {

    private static final String DEMOGRAPHICS_NODE =
            '\\\\xtrials\\Across Trials\\Demographics\\'
    private static final String FEMALE_NODE = // node with non-mobile ova
            '\\\\xtrials\\Across Trials\\Demographics\\Sex\\Female\\'

    AcrossTrialsTestData testData

    def sessionFactory

    @Before
    void setUp() {
        testData = AcrossTrialsTestData.createDefault()
        testData.saveAll()
        sessionFactory.currentSession.flush()
    }

    @Test
    void testGetTopNodeChildren() {
        def result = topNode.children

        assertThat result, contains(
                hasProperty('key', equalTo(DEMOGRAPHICS_NODE))
        )
    }

    @Test
    void testGetTopNodeAllDescendants() {
        def result = topNode.allDescendants

        /* has stuff from all levels */
        assertThat result, hasItem(hasProperty('level', is(1)))
        assertThat result, hasItem(hasProperty('level', is(2)))
        assertThat result, hasItem(hasProperty('level', is(3)))
    }

    @Test
    void testOntologyTermProperties() {
        def modifier = ModifierDimensionView.get('\\Demographics\\Sex\\Female\\')
        assertThat modifier, is(notNullValue())

        OntologyTerm term =
                new AcrossTrialsOntologyTerm(modifierDimension: modifier)

        assertThat term, FastMatchers.propsWith(
                level: 3,
                key: FEMALE_NODE,
                fullName: new ConceptKey(FEMALE_NODE).conceptFullName.toString(),
                study: is(nullValue()),
                name: 'Female',
                code: term.code,
                tooltip: 'Female',
                visualAttributes: contains(LEAF),
                metadata: hasEntry(is('okToUseValues'), is(false)))
    }


    OntologyTerm getTopNode() {
        new AcrossTrialsTopTerm()
    }
}
