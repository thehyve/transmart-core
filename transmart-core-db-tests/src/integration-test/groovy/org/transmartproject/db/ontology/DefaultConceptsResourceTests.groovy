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
import org.transmartproject.core.concept.ConceptKey
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.junit.Assert.fail
import static org.transmartproject.db.ontology.ConceptTestData.addI2b2
import static org.transmartproject.db.ontology.ConceptTestData.addTableAccess

@TestMixin(RuleBasedIntegrationTestMixin)
class DefaultConceptsResourceTests {

    ConceptsResource conceptsResourceService = new DefaultConceptsResource()

    @Before
    void setUp() {
        addTableAccess(level: 0, fullName: '\\foo\\', name: 'foo',
                tableCode: 'i2b2 main', tableName: 'i2b2')
        addTableAccess(level: 1, fullName: '\\foo\\level1', name: 'level1',
                tableCode: 'i2b2 level1', tableName: 'i2b2')
        addTableAccess(level: 0, fullName: '\\hidden\\', name: 'hidden',
               tableCode: 'i2b2 2nd', tableName: 'i2b2',
                cVisualattributes: 'FH')
        addTableAccess(level: 0, fullName: '\\synonym\\', name: 'synonym',
                tableCode: 'i2b2 3rd', tableName: 'i2b2',
                cSynonymCd: 'Y')
        addTableAccess(level: 0, fullName: '\\very bogus\\', name: 'bogus',
                tableCode: 'bogus code', tableName: 'bogus',
                cSynonymCd: 'Y')

        addI2b2(level: 1, fullName: '\\foo\\bar\\', name: 'bar')
    }

    @Test
    void testGetAllCategories() {
        assertThat conceptsResourceService.allCategories, allOf(
                hasItem(hasProperty('name', equalTo('foo'))),
                hasItem(hasProperty('name', equalTo('level1'))),
                hasItem(hasProperty('name', equalTo('hidden'))),
                hasItem(hasProperty('name', equalTo('synonym'))),
        )
    }

    @Test
    void testGetByKeySimple() {
        def concept = conceptsResourceService.getByKey('\\\\i2b2 main' +
                '\\foo\\bar')
        assertThat concept, hasProperty('name', equalTo('bar'))
    }

    @Test
    void testGetByKeyBogusTableCode() {
        def keys = [
                "\\\\does not exist\\foo\\bar\\",
                "\\\\bogus code\\foo\\bar\\"
        ]
        keys.each {
            try {
                conceptsResourceService.getByKey(it)
                fail "There should've been an exception"
            } catch (e) {
                assertThat e, allOf(
                        isA(NoSuchResourceException),
                        hasProperty('message', allOf(
                                containsString('Unknown or unmapped table code'),
                                containsString(new ConceptKey(it).tableCode)
                )))
            }
        }

    }

    @Test
    void testNoSuchFullName() {
        try {
            conceptsResourceService.getByKey('\\\\i2b2 main\\does not exist\\')
            fail "There should've been an exception"
        } catch (e) {
            assertThat e, allOf(
                    isA(NoSuchResourceException),
                    hasProperty('message', allOf(
                            containsString('No non-synonym concept with ' +
                                    'fullName'),
                            containsString('\\does not exist\\')
                    )))
        }
    }
}
