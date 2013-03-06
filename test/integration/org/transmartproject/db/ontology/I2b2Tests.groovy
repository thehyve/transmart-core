package org.transmartproject.db.ontology

import org.transmartproject.core.ontology.OntologyTerm

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import org.junit.*

class I2b2Tests {

    @Before
    void setUp() {
        def common = [
                factTableColumn      :   '',
                dimensionTableName   :   '',
                columnName           :   '',
                columnDataType       :   '',
                operator             :   '',
                dimensionCode        :   '',
                mAppliedPath         :   '',
                updateDate           :   new Date(),
        ]

        def objects = [
                new I2b2(level: 0, fullName: '\\foo\\bar', name: 'var',
                        cVisualattributes: 'FH'),
                new I2b2(level: 0, fullName: '\\foo\\xpto', name: 'xpto')]
        objects.each { obj ->
            common.each { obj."$it.key" = it.value }
            assert obj.save() != null
        }
    }

    @Test
    void testGetVisualAttributes() {
        def terms = I2b2.withCriteria { eq 'level', 0 }

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
}
