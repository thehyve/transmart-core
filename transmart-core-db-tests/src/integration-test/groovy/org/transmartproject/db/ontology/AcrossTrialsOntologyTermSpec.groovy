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
import org.thehyve.commons.test.FastMatchers
import org.transmartproject.core.concept.ConceptKey
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.db.TestData
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.LEAF

@Integration
@Rollback
class AcrossTrialsOntologyTermSpec extends Specification {

    private static final String DEMOGRAPHICS_NODE =
            '\\\\xtrials\\Across Trials\\Demographics\\'
    private static final String FEMALE_NODE = // node with non-mobile ova
            '\\\\xtrials\\Across Trials\\Demographics\\Sex\\Female\\'

    AcrossTrialsTestData testData

    def sessionFactory

    void setupData() {
        TestData.prepareCleanDatabase()

        testData = AcrossTrialsTestData.createDefault()
        testData.saveAll()
    }

    void testGetTopNodeChildren() {
        setupData()
        def result = topNode.children

        expect:
        result contains(
                hasProperty('key', equalTo(DEMOGRAPHICS_NODE))
        )
    }

    void testGetTopNodeAllDescendants() {
        setupData()
        def result = topNode.allDescendants

        /* has stuff from all levels */
        expect:
        result hasItem(hasProperty('level', is(1)))
        result hasItem(hasProperty('level', is(2)))
        result hasItem(hasProperty('level', is(3)))
    }

    void testOntologyTermProperties() {
        setupData()
        when:
        def modifier = ModifierDimensionView.get('\\Demographics\\Sex\\Female\\')
        then:
        modifier is(notNullValue())

        when:
        OntologyTerm term =
                new AcrossTrialsOntologyTerm(modifierDimension: modifier)

        then:
        term FastMatchers.propsWith(
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
