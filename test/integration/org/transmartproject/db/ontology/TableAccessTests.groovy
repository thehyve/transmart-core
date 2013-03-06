package org.transmartproject.db.ontology

import org.transmartproject.core.ontology.OntologyTerm

import org.junit.*
import static org.junit.Assert.fail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@Mixin(ConceptTestData)
class TableAccessTests {

    @Before
    void setUp() {
        addI2b2(level: 1, fullName: '\\foo\\xpto\\', name: 'xpto')
        addI2b2(level: 1, fullName: '\\foo\\bar\\', name: 'var',
                        cVisualattributes: 'FH')
        addI2b2(level: 1, fullName: '\\foo\\baz\\', name: 'baz',
                cSynonymCd: 'Y')
        addI2b2(level: 0, fullName: '\\foo\\', name: 'foo')
        addTableAccess(level: 0, fullName: '\\foo\\', name: 'foo',
                        tableCode: 'i2b2 main', tableName: 'i2b2')
        addTableAccess(level: 0, fullName: '\\fooh\\', name: 'fooh',
                        tableCode: 'bogus table', tableName: 'bogus')
        addTableAccess(level: 0, fullName: '\\notini2b2\\',
                        name: 'notini2b2', tableCode: 'i2b2 2nd code',
                        tableName: 'i2b2')
    }

    @Test
    void testBogusTable() {
        def bogusEntry = TableAccess.findByName('fooh');
        assert bogusEntry != null

        try {
            bogusEntry.children
            fail('Expected exception here')
        } catch (e) {
            assertThat e, allOf(
                    isA(RuntimeException),
                    hasProperty('message', containsString('table bogus is ' +
                            'not mapped'))
            )
        }
    }

    @Test
    void testCategoryNotAlsoInReferredTable() {
        def notInI2b2 = TableAccess.findByName('notini2b2')

        assert notInI2b2 != null

        try {
            notInI2b2.children
            fail('Expected exception here')
        } catch (e) {
            assertThat e, allOf(
                    isA(RuntimeException),
                    hasProperty('message', containsString('could not find it ' +
                            'in class org.transmartproject.db.ontology.' +
                            'I2b2\'s table (fullname: \\notini2b2\\)'))
            )
        }
    }

    @Test
    void testGetChildren() {
        def cats = TableAccess.getCategories()
        def catFoo = cats.find { it.name == 'foo' }
        assertThat catFoo, is(notNullValue(OntologyTerm))

        assertThat catFoo.children, allOf(
                hasSize(1),
                contains(hasProperty('name', equalTo('xpto'))))

        /* show hidden as well */
        assertThat catFoo.getChildren(true), allOf(
                hasSize(2),
                contains(
                        hasProperty('name', equalTo('var')),
                        hasProperty('name', equalTo('xpto'))
                ))

        /* show also synonyms */
        assertThat catFoo.getChildren(true, true), allOf(
                hasSize(3),
                contains( /* ordered by name */
                        hasProperty('name', equalTo('baz')),
                        hasProperty('name', equalTo('var')),
                        hasProperty('name', equalTo('xpto')),
                ))
    }
}
