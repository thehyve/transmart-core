package org.transmartproject.db.ontology

import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.db.concept.ConceptKey

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import org.junit.*

@Mixin(ConceptTestData)
class I2b2Tests {

    @Before
    void setUp() {
        addTableAccess(level: 0, fullName: '\\foo\\', name: 'foo',
                tableCode: 'i2b2 table code', tableName: 'i2b2')
        addI2b2(level: 0, fullName: '\\foo\\', name: 'foo')
        addI2b2(level: 1, fullName: '\\foo\\bar', name: 'var',
                cVisualattributes: 'FH')
        addI2b2(level: 1, fullName: '\\foo\\xpto', name: 'xpto')
        addI2b2(level: 2, fullName: '\\foo\\xpto\\bar', name: 'bar')
    }

    @Test
    void testGetVisualAttributes() {
        def terms = I2b2.withCriteria { eq 'level', 1 }

        //currently testing against postgres database,
        // which has already a matching row there
        assertThat terms, hasSize(greaterThanOrEqualTo(2))
        //assertThat terms, hasSize(2)

        assertThat terms, hasItem(allOf(
                hasProperty('name', equalTo('var')),
                hasProperty('visualAttributes', containsInAnyOrder(
                        OntologyTerm.VisualAttributes.FOLDER,
                        OntologyTerm.VisualAttributes.HIDDEN
                ))
        ))
    }

    @Test
    void testGetTableCode() {
        I2b2 bar = I2b2.find { eq('fullName', '\\foo\\xpto\\bar') }

        assertThat(bar.conceptKey, is(equalTo(
                new ConceptKey('i2b2 table code', '\\foo\\xpto\\bar'))))
    }

    @Test
    void testGetChildren() {
        I2b2 xpto = I2b2.find { eq('fullName', '\\foo\\xpto') }
        xpto.setTableCode('i2b2 table code OOOO')

        assertThat xpto, is(notNullValue())

        def children = xpto.children
        assertThat(children, allOf(
                hasSize(1),
                contains(allOf(
                        hasProperty('fullName', equalTo('\\foo\\xpto\\bar')),
                        /* table code is copied from parent: */
                        hasProperty('conceptKey', equalTo(new ConceptKey
                        ('\\\\i2b2 table code OOOO\\foo\\xpto\\bar')))
                ))))
    }
}
