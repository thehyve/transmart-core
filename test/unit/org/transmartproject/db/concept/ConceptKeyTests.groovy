package org.transmartproject.db.concept

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestMixin(GrailsUnitTestMixin)
class ConceptKeyTests {

    @Test
    void basicTest() {
        def conceptKey = new ConceptKey('\\\\tableCode\\full\\thing\\')

        assertThat conceptKey, allOf(
                hasProperty('tableCode', equalTo('tableCode')),
                hasProperty('conceptFullName', hasProperty('length',
                        equalTo(2))))
    }

    @Test
    void basicTestAlternativeConstructor() {
        def conceptKey = new ConceptKey('tableCode', '\\full\\thing\\')

        assert conceptKey == new ConceptKey('\\\\tableCode\\full\\thing\\')
    }

    @Test
    void badInput() {
        def keys = ['', '\\\\', '\\\\\\', '\\\\a\\', '\\as\\b',
                '\\\\a\\b\\\\', null]

        keys.each({
            try {
                new ConceptKey(it)
                fail "Could create ConceptKey with value " + it
            } catch (Exception iae) {
                assertThat iae, isA(IllegalArgumentException)
            }
        })
    }

}
