package org.transmartproject.db.ontology

import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.db.concept.ConceptKey

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertThat
import static org.junit.Assert.fail
import org.junit.*

@Mixin(ConceptTestData)
class ConceptsResourceServiceTests extends GroovyTestCase {

    ConceptsResource conceptsResourceService

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
